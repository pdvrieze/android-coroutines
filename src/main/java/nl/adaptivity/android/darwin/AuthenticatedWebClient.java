package nl.adaptivity.android.darwin;

import android.accounts.*;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Date;


/**
 * A class making it easier to make authenticated requests to darwin.
 *
 * @todo Make this independent of the apache client libraries and use http urlconnection instead.
 */
public class AuthenticatedWebClient {

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

  private DefaultHttpClient mHttpClient;

  private final String mAuthbase;

  private final Account mAccount;

  public AuthenticatedWebClient(final Context context, final String authbase) {
    this(context, null, authbase);
  }

  public AuthenticatedWebClient(final Context context, final Account account, final String authbase) {
    mContext = context;
    mAccount = account;
    mAuthbase = authbase;
  }

  public HttpResponse execute(final HttpUriRequest request) throws IOException {
    return execute(request, false);
  }

  private HttpResponse execute(final HttpUriRequest request, final boolean retry) throws IOException {
    final AccountManager accountManager =AccountManager.get(mContext);
    mToken = getAuthToken(accountManager, mAuthbase);
    if (mToken==null) { return null; }

    if (mHttpClient==null) { mHttpClient = new DefaultHttpClient(); }

    URI cookieUri = request.getURI();
    cookieUri = cookieUri.resolve("/");

    mHttpClient.getCookieStore().addCookie(new DarwinCookie(cookieUri.toString(), mToken, request.getURI().getPort()));

    final HttpResponse result = mHttpClient.execute(request);
    if (result.getStatusLine().getStatusCode()==HttpURLConnection.HTTP_UNAUTHORIZED) {
      result.getEntity().consumeContent(); // make sure to consume the entire error.
      if (! retry) { // Do not repeat retry
        accountManager.invalidateAuthToken(ACCOUNT_TYPE, mToken);
        return execute(request, true);
      }
    }
    return result;
  }

  @SuppressWarnings("deprecation")
  private String getAuthToken(final AccountManager accountManager, final String authbase) {
    if (mToken != null) return mToken;

    final Account account = mAccount !=null ? mAccount : ensureAccount(mContext, authbase);
    if (account==null) { mAskedForNewAccount = true; }



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
    if (mContext instanceof Activity) {
      result = accountManager.getAuthToken(account, ACCOUNT_TOKEN_TYPE, null, (Activity) mContext, callback , null);
    } else {
      result = accountManager.getAuthToken(account, ACCOUNT_TOKEN_TYPE, true, callback, null);
    }
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

  void writeToBundle(final Bundle dest) {
    dest.putBoolean(KEY_ASKED_FOR_NEW, mAskedForNewAccount);
  }

  void updateFromBundle(final Bundle source) {
    if (source==null) return;
    mAskedForNewAccount = source.getBoolean(KEY_ASKED_FOR_NEW, false);
  }

  public static Account ensureAccount(final Context context, final String source) {
    final AccountManager accountManager = AccountManager.get(context);
    Account[] accounts;
    try {
      accounts = accountManager.getAccountsByTypeAndFeatures(ACCOUNT_TYPE, new String[] {source}, null, null).getResult();
    } catch (OperationCanceledException | AuthenticatorException | IOException e1) {
      if (e1 instanceof AuthenticatorException && "bind failure".equals(e1.getMessage())) {
        accounts = new Account[0]; // no accounts present at all, so just register a new one.
      } else {
        Log.e(TAG, "Failure to get account", e1);
        return null;
      }
    }
    if (accounts.length==0) {
      final Bundle options;
      if (source==null) {
        options = null;
      } else {
        options = new Bundle(1);
        final Uri uri = Uri.parse(source);
        final String authbase = getAuthBase(uri);
        options.putString(KEY_AUTH_BASE, authbase);
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
        final String[] features = new String[] { source} ;
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
            return candidate;
          }
        }
        return null;
      }
      return null;
    }
    return accounts[0];
  }

  /**
   * Convenience method around {@link #getAuthBase(Uri)}
   * @see #getAuthBase(Uri)
   */
  public static String getAuthBase(final String uri) {
    return getAuthBase(uri==null? null: Uri.parse(uri));
  }

  /**
   * Get the authentication base to use. use the original scheme and host, and then use the default port and "/accounts/" as the base.
   * @param uri The base url to use
   * @return The resulting url to use.
   */
  public static String getAuthBase(final Uri uri) {
    if (uri==null) { return null; }
    return uri.getScheme()+"://"+uri.getHost()+"/accounts/";
  }

}
