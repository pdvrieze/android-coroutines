package nl.adaptivity.android.darwin;

import android.accounts.*;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.util.Log;
import org.apache.http.cookie.Cookie;

import javax.net.ssl.HttpsURLConnection;

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;


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

  private static class DarwinCookie implements Cookie {

    private static final int[] PORTS = new int[] {443};
    private final String mAuthToken;
    private final String mAuthbase;
    private final int mPort;

    public DarwinCookie(final String authbase, final String authtoken, final int port) {
      mAuthbase = authbase;
      mAuthToken = authtoken;
      mPort = port;
    }

    @Override
    public String getComment() {
      return null;
    }

    @Override
    public String getCommentURL() {
      return null;
    }

    @Override
    public String getDomain() {
      return Uri.parse(mAuthbase).getHost();
    }

    @Override
    public Date getExpiryDate() {
      return null;
    }

    @Override
    public String getName() {
      return DARWIN_AUTH_COOKIE;
    }

    @Override
    public String getPath() {
      return "/";
    }

    @Override
    public int[] getPorts() {
      return new int[] { mPort };
    }

    @Override
    public String getValue() {
      return mAuthToken;
    }

    @Override
    public int getVersion() {
      return 1;
    }

    @Override
    public boolean isExpired(final Date date) {
      return false;
    }

    @Override
    public boolean isPersistent() {
      return false;
    }

    @Override
    public boolean isSecure() {
      return "https".equals(Uri.parse(mAuthbase).getScheme());
    }

  }

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
    return execute(request, false);
  }

  private HttpURLConnection execute(final WebRequest request, final boolean currentlyInRetry) throws IOException {
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
          return execute(request, true);
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

    Account account = getAccount(authbase);
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
  private Account getAccount(final URI authbase) {
    if (mAccount!=null) {
      return mAccount;
    } else {
      Account[] accounts = getAccount(mContext, authbase);
      if (accounts.length > 1) { throw new IllegalStateException("Multiple accounts given"); }
      Account account = accounts.length == 0 ? null : accounts[0];

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

  public static Account[] ensureAccount(final Activity context, final URI source, int activityRequestCode) {
    if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
      return Api14Helper.ensureAccount(context, source, activityRequestCode);
    }

    final AccountManager accountManager = AccountManager.get(context);
    final Account[] accounts = getAccount(context, source);

    if (accounts.length==0) {
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
        result = accountManager.addAccount(ACCOUNT_TYPE, ACCOUNT_TOKEN_TYPE, null, options, context instanceof Activity ? ((Activity) context) : null, null, null).getResult();
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
            try {
              accountManager.blockingGetAuthToken(candidate, ACCOUNT_TOKEN_TYPE, true);
            } catch (OperationCanceledException | IOException | AuthenticatorException e) {
              e.printStackTrace();
              throw new RuntimeException(e);
            }
            return new Account[]{candidate};
          }
        }
      }
      return new Account[0];
    }
    return accounts;
  }

  /**
   * Convenience method around {@link #getAuthBase(URI)}
   * @see #getAuthBase(URI)
   */
  public static URI getAuthBase(final String uri) {
    return getAuthBase(uri==null? null: URI.create(uri));
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
    final AccountManager accountManager = AccountManager.get(context);
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
    SharedPreferences preferences = context.getSharedPreferences(AuthenticatedWebClient.class.getName(), Context.MODE_PRIVATE);
    final String storedAccountName = preferences.getString(KEY_ACCOUNT_NAME, null);
    if (storedAccountName!=null) {
      for(Account candidate: accounts) {
        if (storedAccountName.equals(candidate.name)) {
          return new Account[]{candidate};
        }
      }
    }
    return new Account[0];
  }

  @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
  private static class Api14Helper {

    public static Account[] ensureAccount(final Activity context, final URI source, final int activityRequestCode) {
      final Account[] accounts = getAccount(context, source);

      if (accounts.length!=1) {
        final Bundle options;
        if (source == null) {
          options = null;
        } else {
          options = new Bundle(1);
          final URI authbase = getAuthBase(source);
          options.putString(KEY_AUTH_BASE, authbase.toString());
        }

        // We didn't find the account we knew about
        Intent intent = AccountManager.newChooseAccountIntent(null, null, new String[]{ACCOUNT_TYPE}, false, null, null, null, options);
        context.startActivityForResult(intent, activityRequestCode);

        return null;
      }
      return accounts;
    }

  }
}
