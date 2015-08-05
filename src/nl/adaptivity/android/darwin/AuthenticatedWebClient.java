package nl.adaptivity.android.darwin;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Date;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;


/**
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
    private final String aAuthToken;
    private String aAuthbase;
    private int aPort;

    public DarwinCookie(String pAuthbase, String pAuthtoken, int pPort) {
      aAuthbase = pAuthbase;
      aAuthToken = pAuthtoken;
      aPort = pPort;
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
      return Uri.parse(aAuthbase).getHost();
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
      return new int[] { aPort };
    }

    @Override
    public String getValue() {
      return aAuthToken;
    }

    @Override
    public int getVersion() {
      return 1;
    }

    @Override
    public boolean isExpired(Date pDate) {
      return false;
    }

    @Override
    public boolean isPersistent() {
      return false;
    }

    @Override
    public boolean isSecure() {
      return "https".equals(Uri.parse(aAuthbase).getScheme());
    }

  }

  private Context mContext;
  private boolean mAskedForNewAccount = false;

  private String mToken = null;

  private DefaultHttpClient mHttpClient;

  private final String mAuthbase;

  private Account mAccount;

  public AuthenticatedWebClient(Context context, String authbase) {
    this(context, null, authbase);
  }

  public AuthenticatedWebClient(Context context, Account account, String authbase) {
    mContext = context;
    mAccount = account;
    mAuthbase = authbase;
  }

  public HttpResponse execute(HttpUriRequest pRequest) throws ClientProtocolException, IOException {
    return execute(pRequest, false);
  }

  private HttpResponse execute(HttpUriRequest pRequest, boolean retry) throws ClientProtocolException, IOException {
    final AccountManager accountManager =AccountManager.get(mContext);
    mToken = getAuthToken(accountManager, mAuthbase);
    if (mToken==null) { return null; }

    if (mHttpClient==null) { mHttpClient = new DefaultHttpClient(); }

    mHttpClient.getCookieStore().addCookie(new DarwinCookie(mAuthbase, mToken, pRequest.getURI().getPort()));

    final HttpResponse result = mHttpClient.execute(pRequest);
    if (result.getStatusLine().getStatusCode()==HttpURLConnection.HTTP_UNAUTHORIZED) {
      result.getEntity().consumeContent(); // make sure to consume the entire error.
      if (! retry) { // Do not repeat retry
        accountManager.invalidateAuthToken(ACCOUNT_TYPE, mToken);
        return execute(pRequest, true);
      }
    }
    return result;
  }

  @SuppressWarnings("deprecation")
  private String getAuthToken(AccountManager accountManager, String authbase) {
    if (mToken != null) return mToken;

    Account account = mAccount !=null ? mAccount : ensureAccount(mContext, authbase);
    if (account==null) { mAskedForNewAccount = true; }



    AccountManagerCallback<Bundle> callback = new AccountManagerCallback<Bundle>() {

      @Override
      public void run(AccountManagerFuture<Bundle> pFuture) {
        try {
          Bundle b = pFuture.getResult();
          if (b.containsKey(AccountManager.KEY_INTENT)) {
            Intent i = (Intent) b.get(AccountManager.KEY_INTENT);
            mContext.startActivity(i);
          }
        } catch (OperationCanceledException e) {
          e.printStackTrace();
        } catch (AuthenticatorException e) {
          e.printStackTrace();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    };
    AccountManagerFuture<Bundle> result;
    if (mContext instanceof Activity) {
      result = accountManager.getAuthToken(account, ACCOUNT_TOKEN_TYPE, null, (Activity) mContext, callback , null);
    } else {
      result = accountManager.getAuthToken(account, ACCOUNT_TOKEN_TYPE, true, callback, null);
    }
    Bundle b;
    try {
//      return accountManager.blockingGetAuthToken(account, ACCOUNT_TOKEN_TYPE, false);
      b = result.getResult();
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
    return b.getString(AccountManager.KEY_AUTHTOKEN);

  }

  void writeToBundle(Bundle pDest) {
    pDest.putBoolean(KEY_ASKED_FOR_NEW, mAskedForNewAccount);
  }

  void updateFromBundle(Bundle pSource) {
    if (pSource==null) return;
    mAskedForNewAccount = pSource.getBoolean(KEY_ASKED_FOR_NEW, false);
  }

  public static Account ensureAccount(Context pContext, String pSource) {
    AccountManager accountManager = AccountManager.get(pContext);
    Account[] accounts;
    try {
      accounts = accountManager.getAccountsByTypeAndFeatures(ACCOUNT_TYPE, new String[] {pSource}, null, null).getResult();
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
      if (pSource==null) {
        options = null;
      } else {
        options = new Bundle(1);
        Uri uri = Uri.parse(pSource);
        String authbase = getAuthBase(uri);
        options.putString(KEY_AUTH_BASE, authbase);
      }
      Bundle result;
      try {
        result = accountManager.addAccount(ACCOUNT_TYPE, ACCOUNT_TOKEN_TYPE, null, options, pContext instanceof Activity ? ((Activity) pContext) : null, null, null).getResult();
      } catch (OperationCanceledException | AuthenticatorException | IOException e) {
        return null;
      }
      if (result.containsKey(AccountManager.KEY_INTENT)) {
        return null;
//        pContext.startActivity(result.<Intent>getParcelable(AccountManager.KEY_INTENT));
      } else if (result.containsKey(AccountManager.KEY_ACCOUNT_NAME)) {
        String[] features = new String[] { pSource} ;
        String name = result.getString(AccountManager.KEY_ACCOUNT_NAME);
        Account[] candidates;
        try {
          candidates = accountManager.getAccountsByTypeAndFeatures(ACCOUNT_TYPE, features, null, null).getResult();
        } catch (OperationCanceledException | AuthenticatorException | IOException e) {
          return null;
        }
        for(Account candidate:candidates) {
          if (name.equals(candidate.name)) {
            return candidate;
          }
        }
        return null;
      }
      return null;
    }
    Account account = accounts[0];
    return account;
  }

  public static String getAuthBase(String uri) {
    return getAuthBase(uri==null? null: Uri.parse(uri));
  }

  public static String getAuthBase(Uri uri) {
    if (uri==null) { return null; }
    return uri.getScheme()+"://"+uri.getHost()+"/accounts/";
  }

}
