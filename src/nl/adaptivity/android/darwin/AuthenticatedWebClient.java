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
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;



public class AuthenticatedWebClient {

  private static final String TAG = "ACCOUNTINFO";

  private static final String ACCOUNT_TYPE = "uk.ac.bournemouth.darwin.account";

  public static final String ACCOUNT_TOKEN_TYPE="uk.ac.bournemouth.darwin.auth";

  private static final String KEY_ASKED_FOR_NEW = "askedForNewAccount";

  private static final String DARWIN_AUTH_COOKIE = "DWNID";

  private static class DarwinCookie implements Cookie {

    private static final int[] PORTS = new int[] {443};
    private final String aAuthToken;

    public DarwinCookie(String pAuthtoken) {
      aAuthToken = pAuthtoken;
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
      return "darwin.bournemouth.ac.uk";
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
      return PORTS;
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
      return false;
    }

  }

  private Context mContext;
  private boolean mAskedForNewAccount = false;

  private String mToken = null;

  private DefaultHttpClient mHttpClient;

  public AuthenticatedWebClient(Context context) {
    mContext = context;
  }

  public HttpResponse execute(HttpUriRequest pRequest) throws ClientProtocolException, IOException {
    return execute(pRequest, false);
  }

  private HttpResponse execute(HttpUriRequest pRequest, boolean retry) throws ClientProtocolException, IOException {
    final AccountManager accountManager =AccountManager.get(mContext);
    mToken = getAuthToken(accountManager);

    if (mHttpClient==null) { mHttpClient = new DefaultHttpClient(); }

    mHttpClient.getCookieStore().addCookie(new DarwinCookie(mToken));

    final HttpResponse result = mHttpClient.execute(pRequest);
    if (result.getStatusLine().getStatusCode()==HttpURLConnection.HTTP_UNAUTHORIZED) {
      if (! retry) { // Do not repeat retry
        accountManager.invalidateAuthToken(ACCOUNT_TYPE, mToken);
        return execute(pRequest, true);
      }
    }
    return result;
  }

  private String getAuthToken(AccountManager accountManager) {
    if (mToken != null) return mToken;

    Account[] accounts = accountManager.getAccountsByType(ACCOUNT_TYPE);
    if (accounts.length==0) {
      // TODO prompt for download and install of the account manager it it is not available.
      if (mContext instanceof Activity) {
        accountManager.addAccount(ACCOUNT_TYPE, ACCOUNT_TOKEN_TYPE, null, null, (Activity)mContext, null, null);
      } else {
        // TODO something better
        return null;
      }
      mAskedForNewAccount =true;
      return null;
    }
    Account account = accounts[0];

    try {
      return accountManager.blockingGetAuthToken(account, ACCOUNT_TOKEN_TYPE, false);
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
  }

  void writeToBundle(Bundle pDest) {
    pDest.putBoolean(KEY_ASKED_FOR_NEW, mAskedForNewAccount);
  }

  void updateFromBundle(Bundle pSource) {
    if (pSource==null) return;
    mAskedForNewAccount = pSource.getBoolean(KEY_ASKED_FOR_NEW, false);
  }

}
