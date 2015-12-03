package nl.adaptivity.android.darwin;

import android.accounts.*;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.util.Log;

import javax.net.ssl.HttpsURLConnection;

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;


/**
 * A class making it easier to make authenticated requests to darwin.
 *
 * @todo Make this independent of the apache client libraries and use http urlconnection instead.
 */
public class AuthenticatedWebClient {

  public static final String KEY_ACCOUNT_NAME = "ACCOUNT_NAME";

  public static class WebRequest {
    private final URI mUri;

    public WebRequest(final URI uri) {
      String uriScheme = uri.getScheme();
      if (! ("http".equals(uriScheme)|| "https".equals(uriScheme))) {
        throw new IllegalArgumentException("Webrequests only work with http and https schemes");
      }
      mUri = uri;
    }

    public URI getUri() {
      return mUri;
    }

    @CallSuper
    public HttpURLConnection getConnection() throws IOException {
      return (HttpURLConnection) mUri.toURL().openConnection();
    }
  }

  public static class GetRequest extends WebRequest {

    public GetRequest(final URI uri) {
      super(uri);
    }
  }

  public static class DeleteRequest extends WebRequest {

    public DeleteRequest(final URI uri) {
      super(uri);
    }

    @Override
    public HttpURLConnection getConnection() throws IOException {
      HttpURLConnection connection = super.getConnection();
      connection.setRequestMethod("DELETE");
      return connection;
    }
  }

  private static class CharSequenceCallback implements StreamWriterCallback {

    private final CharSequence mBody;

    public CharSequenceCallback(final CharSequence body) {
      mBody = body;
    }

    @Override
    public void writeTo(final OutputStream stream) throws IOException {
      Writer out = new OutputStreamWriter(stream, Charset.forName("UTF-8"));
      for(int i=0; i<mBody.length(); ++i) {
        out.append(mBody);
      }
    }
  }

  public static class PostRequest extends WebRequest {

    private final StreamWriterCallback mWritingCallback;

    public PostRequest(final URI uri, StreamWriterCallback writingCallback) {
      super(uri);
      mWritingCallback = writingCallback;
    }

    public PostRequest(final URI uri, final CharSequence body) {
      this(uri, new CharSequenceCallback(body));
    }

    @Override
    public HttpURLConnection getConnection() throws IOException {
      HttpURLConnection connection = super.getConnection();
      connection.setDoOutput(true);
      connection.setChunkedStreamingMode(0);
      mWritingCallback.writeTo(connection.getOutputStream());
      return connection;
    }
  }

  public interface StreamWriterCallback {
    void writeTo(OutputStream stream) throws IOException;
  }

  private static final String TAG = AuthenticatedWebClient.class.getName();

  public static final String ACCOUNT_TYPE = "uk.ac.bournemouth.darwin.account";

  public static final String ACCOUNT_TOKEN_TYPE="uk.ac.bournemouth.darwin.auth";

  private static final String KEY_ASKED_FOR_NEW = "askedForNewAccount";
  public static final String KEY_AUTH_BASE = "authbase";

  private static final String DARWIN_AUTH_COOKIE = "DWNID";

  private final Context mContext;
  private boolean mAskedForNewAccount = false;

  private String mToken = null;

  private final URI mAuthbase;

  private Account mAccount;
  private CookieManager mCookieManager;

  public AuthenticatedWebClient(final Context context, final URI authbase) {
    this(context, null, authbase);
  }

  public AuthenticatedWebClient(final Context context, final Account account, final URI authbase) {
    mContext = context;
    mAccount = account;
    mAuthbase = authbase;
  }

  public HttpURLConnection execute(final WebRequest request) throws IOException {
    return execute(request, false, -1);
  }

  public HttpURLConnection execute(final WebRequest request, final int activityRequestCode) throws IOException {
    return execute(request, false, activityRequestCode);
  }

  @Nullable
  private HttpURLConnection execute(final WebRequest request, final boolean currentlyInRetry, final int activityRequestCode) throws IOException {
    final AccountManager accountManager =AccountManager.get(mContext);
    mToken = getAuthToken(accountManager, mAuthbase);
    if (mToken==null) { return null; }

    if (mCookieManager==null) {
      mCookieManager = new CookieManager();
      CookieHandler.setDefault(mCookieManager);
    }

    URI cookieUri = request.getUri();
    cookieUri = cookieUri.resolve("/");

    HttpCookie cookie = new HttpCookie(DARWIN_AUTH_COOKIE, mToken);
    cookie.setDomain(cookieUri.getHost());

    HttpURLConnection connection = request.getConnection();
    try {
      if (connection instanceof HttpsURLConnection) {
        cookie.setSecure(true);
      }
      mCookieManager.getCookieStore().add(cookieUri, cookie);


      if (connection.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
        InputStream errorStream = connection.getErrorStream();
        try {
          errorStream.skip(Integer.MAX_VALUE);
        } finally {
          errorStream.close();
        }
        if (!currentlyInRetry) { // Do not repeat retry
          accountManager.invalidateAuthToken(ACCOUNT_TYPE, mToken);
          int activityRequestCode1 = -1;
          return execute(request, true, activityRequestCode1);
        }
      }
      return connection;
    } catch (Throwable e) { // Catch exception as there would be no way for a caller to disconnect
      connection.disconnect();
      throw e;
    }
  }

  @SuppressWarnings("deprecation")
  private String getAuthToken(final AccountManager accountManager, final URI authbase) {
    if (mToken != null) return mToken;

    Account account = getAccount(accountManager, authbase);
    if (account == null) return null;



    final AccountManagerCallback<Bundle> callback = new AccountManagerCallback<Bundle>() {

      @Override
      public void run(final AccountManagerFuture<Bundle> future) {
        try {
          final Bundle bundle = future.getResult();
          if (bundle.containsKey(AccountManager.KEY_INTENT)) {
            final Intent intent = (Intent) bundle.get(AccountManager.KEY_INTENT);
            mContext.startActivity(intent);
          }
        } catch (OperationCanceledException | AuthenticatorException | IOException e) {
          Log.d(TAG, "Failed to get account", e);
        }
      }
    };
    final AccountManagerFuture<Bundle> result;
    result = accountManager.getAuthToken(account, ACCOUNT_TOKEN_TYPE, true, callback, null);

    final Bundle bundle;
    try {
//      return accountManager.blockingGetAuthToken(account, ACCOUNT_TOKEN_TYPE, false);
      bundle = result.getResult();
    } catch (OperationCanceledException e) {
      Log.e(TAG, "Error logging in: ", e);
      return null;
    } catch (AuthenticatorException e) {
      Log.e(TAG, "Error logging in: ", e);
      return null;
    } catch (IOException e) {
      Log.e(TAG, "Error logging in: ", e);
      return null;
    }
    return bundle.getString(AccountManager.KEY_AUTHTOKEN);

  }

  @Nullable
  private Account getAccount(final AccountManager accountManager, final URI authbase) {
    if (mAccount!=null) {
      return mAccount;
    } else {
      Account account = getAccount(accountManager, mContext, authbase);

      if (account == null) { return null; }
      mAccount = account;
      return account;
    }
  }

  public void writeToBundle(final Bundle dest) {
    dest.putBoolean(KEY_ASKED_FOR_NEW, mAskedForNewAccount);
  }

  public void updateFromBundle(final Bundle source) {
    if (source==null) return;
    mAskedForNewAccount = source.getBoolean(KEY_ASKED_FOR_NEW, false);
  }

  public static Account ensureAccount(final Activity context, final URI source, int activityRequestCode) {
    if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
      return Api14Helper.ensureAccount(context, source, activityRequestCode);
    }

    final AccountManager accountManager = AccountManager.get(context);
    final Account account = getAccount(accountManager, context, source);

    if (account==null) {
      final Bundle options;
      if (source==null) {
        options = null;
      } else {
        options = new Bundle(1);
        final URI authbase = getAuthBase(source);
        options.putString(KEY_AUTH_BASE, authbase.toString());
      }
      final Bundle result;
      try {
        result = accountManager.addAccount(ACCOUNT_TYPE, ACCOUNT_TOKEN_TYPE, null, options, context, null, null).getResult();
      } catch (OperationCanceledException | AuthenticatorException | IOException e) {
        return null;
      }
      if (result.containsKey(AccountManager.KEY_INTENT)) {
        return null;
//        pContext.startActivity(result.<Intent>getParcelable(AccountManager.KEY_INTENT));
      } else if (result.containsKey(AccountManager.KEY_ACCOUNT_NAME)) {
        final String[] features = new String[] { source.toString()} ;
        final String name = result.getString(AccountManager.KEY_ACCOUNT_NAME);
        assert name != null : "Name should not be null if it is contained in the intent";
        final Account[] candidates;
        try {
          candidates = accountManager.getAccountsByTypeAndFeatures(ACCOUNT_TYPE, features, null, null).getResult();
        } catch (OperationCanceledException | AuthenticatorException | IOException e) {
          return null;
        }
        for(final Account candidate:candidates) {
          if (name.equals(candidate.name)) {
            storeUsedAccount(context, candidate.name);
            try {
              accountManager.blockingGetAuthToken(candidate, ACCOUNT_TOKEN_TYPE, true);
            } catch (OperationCanceledException | IOException | AuthenticatorException e) {
              e.printStackTrace();
              throw new RuntimeException(e);
            }
            return candidate;
          }
        }
      }
      return null;
    }
    return account;
  }

  /**
   * Convenience method around {@link #getAuthBase(URI)}
   * @see #getAuthBase(URI)
   */
  public static URI getAuthBase(final String uri) {
    return getAuthBase(uri == null ? null : URI.create(uri));
  }

  /**
   * Get the authentication base to use. use the original scheme and host, and then use the default port and "/accounts/" as the base.
   * @param uri The base url to use
   * @return The resulting url to use.
   */
  public static URI getAuthBase(final URI uri) {
    if (uri==null) { return null; }
    return URI.create(uri.getScheme() + "://" + uri.getHost() + "/accounts/");
  }

  public static Account[] getAccount(final Context context, final URI source) {
    return getAccounts(AccountManager.get(context), context, source);
  }

  public static Account getAccount(final AccountManager accountManager, final Context context, final URI source) {
    Account [] accounts = getAccounts(accountManager, context, source);
    final String storedAccountName = getStoredAccountName(context);
    if (storedAccountName!=null) {
      for(Account candidate: accounts) {
        if (storedAccountName.equals(candidate.name)) {
          return candidate;
        }
      }
    }
    return null;
  }

  private static String getStoredAccountName(final Context context) {SharedPreferences preferences = context.getSharedPreferences(AuthenticatedWebClient.class.getName(), Context.MODE_PRIVATE);
    return preferences.getString(KEY_ACCOUNT_NAME, null);
  }

  public static Account[] getAccounts(final AccountManager accountManager, final Context context, final URI source) {
    Account[] accounts;
    try {
      accounts = accountManager.getAccountsByTypeAndFeatures(ACCOUNT_TYPE, source==null ? null : new String[] {source.toString()}, null, null).getResult();
    } catch (OperationCanceledException | AuthenticatorException | IOException e1) {
      if (e1 instanceof AuthenticatorException && "bind failure".equals(e1.getMessage())) {
        return new Account[0]; // no accounts present at all, so just register a new one.
      } else {
        Log.e(TAG, "Failure to get account", e1);
        throw new RuntimeException(e1);
      }
    }
    return accounts;
  }

  public static void storeUsedAccount(Context context, String accountname) {
    SharedPreferences preferences = context.getSharedPreferences(AuthenticatedWebClient.class.getName(), Context.MODE_PRIVATE);
    final SharedPreferences.Editor editor = preferences.edit();
    editor.putString(KEY_ACCOUNT_NAME, accountname);
    editor.apply();
  }

  @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
  private static class Api14Helper {

    public static Account ensureAccount(final Activity context, final URI source, final int activityRequestCode) {
      final AccountManager accountManager = AccountManager.get(context);
      final Account[] accounts = getAccounts(accountManager, context, source);
      String accountName = getStoredAccountName(context);
      if (accountName!=null && accounts.length>0) {
        for (Account account: accounts) { if (accountName.equals(account.name)) { return account; }}
      }

      final Bundle options;
      if (source == null) {
        options = null;
      } else {
        options = new Bundle(1);
        final URI authbase = getAuthBase(source);
        options.putString(KEY_AUTH_BASE, authbase.toString());
      }
      if (accounts.length>=1) {
        // We didn't find the account we knew about
        Intent intent = AccountManager.newChooseAccountIntent(null, null, new String[]{ACCOUNT_TYPE}, false, null, null, null, options);
        context.startActivityForResult(intent, activityRequestCode);
      } else {
        final Bundle result;
        try {
          result = accountManager.addAccount(ACCOUNT_TYPE, ACCOUNT_TOKEN_TYPE, null, options, context, null, null).getResult();
        } catch (OperationCanceledException | AuthenticatorException | IOException e) {
          return null;
        }
        if (result.containsKey(AccountManager.KEY_INTENT)) {
          return null;
//        pContext.startActivity(result.<Intent>getParcelable(AccountManager.KEY_INTENT));
        }
      }

      return null;
    }

  }
}
