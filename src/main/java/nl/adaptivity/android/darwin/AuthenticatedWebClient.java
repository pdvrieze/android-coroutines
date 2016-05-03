/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 2.1 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.android.darwin;

import android.Manifest.permission;
import android.accounts.*;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import nl.adaptivity.android.darwinlib.R;

import javax.net.ssl.HttpsURLConnection;

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.util.ArrayList;


/**
 * A class making it easier to make authenticated requests to darwin.
 *
 * @todo Make this independent of the apache client libraries and use http urlconnection instead.
 */
public class AuthenticatedWebClient {

  public static final String KEY_ACCOUNT_NAME = "ACCOUNT_NAME";

  public static class WebRequest {
    private final URI mUri;
    private String[] mHeaders;
    private int mHeaderCount;

    public WebRequest(final URI uri) {
      String uriScheme = uri.getScheme();
      if (! ("http".equals(uriScheme)|| "https".equals(uriScheme))) {
        throw new IllegalArgumentException("Webrequests only work with http and https schemes");
      }
      mUri = uri;
    }

    public void setHeader(final String name, final String value) {
      if (mHeaders==null) {
        mHeaders = new String[10];
      } else if (mHeaders.length==mHeaderCount*2) {
        String[] tmp = new String[mHeaders.length * 2];
        System.arraycopy(mHeaders, 0, tmp, 0, mHeaders.length);
        mHeaders = tmp;
      }
      int pos = mHeaderCount*2;
      mHeaders[pos] = name;
      mHeaders[pos+1] = value;
      mHeaderCount++;
    }

    public String getHeaderName(int index) {
      return mHeaders[index*2];
    }

    public String getHeaderValue(int index) {
      return mHeaders[index*2 + 1];
    }

    public int getHeaderCount() {
      return mHeaderCount;
    }

    public URI getUri() {
      return mUri;
    }

    @CallSuper
    public HttpURLConnection getConnection() throws IOException {
      HttpURLConnection connection = (HttpURLConnection) mUri.toURL().openConnection();
      for (int i = 0; i < mHeaderCount; i++) {
        connection.addRequestProperty(mHeaders[i * 2], mHeaders[i * 2 + 1]);
      }
      return connection;
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
      try {
        out.append(mBody);
      } finally {
        out.flush(); // Just flush, don't close.
      }
    }
  }

  public static class PostRequest extends WebRequest {

    private final StreamWriterCallback mWritingCallback;

    public PostRequest(final URI uri, StreamWriterCallback writingCallback) {
      super(uri);
      setContentType("application/binary"); // This is a much better default than form/urlencoded
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
      OutputStream outputStream = connection.getOutputStream();
      try {
        mWritingCallback.writeTo(outputStream);
      } finally {
        outputStream.close();
      }
      return connection;
    }

    public void setContentType(final String contentType) {
      setHeader("Content-type", contentType);
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
  public static final String DOWNLOAD_DIALOG_TAG = "DOWNLOAD_DIALOG";
  private static String _sharedPreferenceName = AuthenticatedWebClient.class.getName();

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
    Log.d(TAG, "execute() called with: " + "request = [" + request + "], currentlyInRetry = [" + currentlyInRetry + "], activityRequestCode = [" + activityRequestCode + "]");
    final AccountManager accountManager =AccountManager.get(mContext);
    mToken = getAuthToken(accountManager, mAuthbase);
    if (mToken==null) { return null; }

    if (mCookieManager==null) {
      mCookieManager = new CookieManager(new MyCookieStore(), CookiePolicy.ACCEPT_ORIGINAL_SERVER);
      CookieHandler.setDefault(mCookieManager);
    }

    URI cookieUri = request.getUri();
//    cookieUri = cookieUri.resolve("/");

    HttpCookie cookie = new HttpCookie(DARWIN_AUTH_COOKIE, mToken);
    cookie.setDomain(cookieUri.getHost());
//    cookie.setVersion(1);
//    cookie.setPath("/");

    HttpURLConnection connection = request.getConnection();
    connection.setRequestProperty(DARWIN_AUTH_COOKIE, mToken);
    try {
      if (connection instanceof HttpsURLConnection) {
        cookie.setSecure(true);
      }
      CookieStore cookieStore = mCookieManager.getCookieStore();
      removeConflictingCookies(cookieStore, cookie);
      cookieStore.add(cookieUri, cookie);


      if (connection.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
        InputStream errorStream = connection.getErrorStream();
        try {
          errorStream.skip(Integer.MAX_VALUE);
        } finally {
          errorStream.close();
        }
        Log.d(TAG, "execute: Invalidating auth token");
        accountManager.invalidateAuthToken(ACCOUNT_TYPE, mToken);
        if (!currentlyInRetry) { // Do not repeat retry
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

  private void removeConflictingCookies(final CookieStore cookieStore, final HttpCookie refCookie) {
    ArrayList<HttpCookie> toRemove = new ArrayList<>();
    for(HttpCookie existingCookie:cookieStore.getCookies()) {
      if (existingCookie.getName().equals(refCookie.getName())) toRemove.add(existingCookie);
    }
    for(HttpCookie c:toRemove) {
      cookieStore.remove(null, c);
      for(URI url: cookieStore.getURIs()) {
        cookieStore.remove(url, c);
      }
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
            if (mContext instanceof Activity) {
              mContext.startActivity(intent);
            } else {
              PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, 0);
              String        contentInfo   = mContext.getString(R.string.notification_authreq_contentInfo);
              String        actionLabel   = mContext.getString(R.string.notification_authreq_action);
              Notification  notification  = new NotificationCompat
                                                        .Builder(mContext)
                                                        .setSmallIcon(R.drawable.ic_notification)
                                                        .setContentInfo(contentInfo)
                                                        .setContentIntent(pendingIntent)
                                                        .addAction(R.drawable.ic_notification, actionLabel, pendingIntent)
                                                        .build();
              NotificationManagerCompat.from(mContext).notify(0, notification);
            }
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

  public static Account ensureAccount(final Activity context, final URI source) {
    return ensureAccount(context, source, -1, -1);
  }

  public static Account ensureAccount(final Activity context, final URI source, int chooseRequestCode) {
    return ensureAccount(context, source, chooseRequestCode, -1);
  }

  public static Account ensureAccount(final Activity context, final URI source, int chooseRequestCode, int downloadRequestCode) {
    if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
      return Api14Helper.ensureAccount(context, source, chooseRequestCode, downloadRequestCode);
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
    return uri.resolve("/accounts/");
  }

  /**
   * Get candidate accounts known to the account manager.
   * @param context The context to use.
   * @param source The authentication url to use.
   * @return A list of current candidate acccounts.
   * @see {@link #getCandidateAccounts(AccountManager, Context, URI)}
   */
  @WorkerThread
  public static Account[] getCandidateAccounts(final Context context, final URI source) {
    return getCandidateAccounts(AccountManager.get(context), context, source);
  }

  /**
   * Get (and verify) the stored account. If the stored account no longer exists, it will be removed
   * from the preferences.
   * @param accountManager The account manager to use
   * @param context The context to use
   * @param source The authentication url to use. Use null for the default.
   * @return The account.
   */
  @WorkerThread
  public static Account getAccount(final AccountManager accountManager, final Context context, final URI source) {
    final String storedAccountName = getStoredAccountName(context);
    if (storedAccountName!=null) {
      Account [] accounts = getCandidateAccounts(accountManager, context, source);
      for(Account candidate: accounts) {
        if (storedAccountName.equals(candidate.name)) {
          return candidate;
        }
      }
      // The stored account name is no longer valid
      storeUsedAccount(context, null);
    }
    return null;
  }

  private static String getStoredAccountName(final Context context) {
    SharedPreferences preferences = getSharedPreferences(context);
    return preferences.getString(KEY_ACCOUNT_NAME, null);
  }

  /**
   * Get an unverified account object for the account to use. This can be used from the UI thread as
   * there is no verification against the account manager.
   * @param context The context to use.
   * @return An account.
   */
  public static Account getStoredAccount(final Context context) {
    final String accountName = getStoredAccountName(context);
    return TextUtils.isEmpty(accountName) ? null : new Account(accountName, ACCOUNT_TYPE);
  }

  public static String getSharedPreferenceName() {
    return _sharedPreferenceName;
  }

  public static void setSharedPreferenceName(String name) {
    _sharedPreferenceName=name;
  }

  private static SharedPreferences getSharedPreferences(final Context context) {
    if (_sharedPreferenceName==null) {
      return PreferenceManager.getDefaultSharedPreferences(context);
    } else {
      return context.getSharedPreferences(_sharedPreferenceName, Context.MODE_PRIVATE);
    }
  }

  /**
   * Get candidate accounts known to the account manager.
   * @param accountManager The account manager to use.
   * @param context The context to use.
   * @param source The authentication url to use.
   * @return A list of current candidate acccounts.
   */
  @WorkerThread
  public static Account[] getCandidateAccounts(final AccountManager accountManager, final Context context, final URI source) {
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

  /**
   * Record the used account name in a shared preferences file.
   * @param context The context to use to read the shared preferences.
   * @param accountname The name of the account.
   */
  public static void storeUsedAccount(Context context, String accountname) {
    SharedPreferences preferences = getSharedPreferences(context);
    final SharedPreferences.Editor editor = preferences.edit();
    if (accountname==null || accountname.isEmpty()) {
      editor.remove(KEY_ACCOUNT_NAME);
    } else {
      editor.putString(KEY_ACCOUNT_NAME, accountname);
    }
    editor.apply();
  }

  private static boolean hasAuthenticator(AccountManager accountManager) {
    for(AuthenticatorDescription authenticator: accountManager.getAuthenticatorTypes()) {
      if (ACCOUNT_TYPE.equals(authenticator.type)) {
        return true;
      }
    }
    return false;
  }

  private static void startDownloadDialog(Activity activity, final int downloadRequestCode) {
    DownloadDialog downloadDialog = DownloadDialog.newInstance(downloadRequestCode);

    downloadDialog.show(activity.getFragmentManager(), DOWNLOAD_DIALOG_TAG);
  }

  @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
  private static class Api14Helper {

    private static final int MY_PERMISSIONS_REQUEST_GET_ACCOUNTS = 254;

    public static Account ensureAccount(final Activity activity, final URI source, final int chooseRequestCode, final int downloadRequestCode) {
      Log.d(TAG, "ensureAccount() called with: " + "activity = [" + activity + "], source = [" + source + "], chooseRequestCode = [" + chooseRequestCode + "], downloadRequestCode = [" + downloadRequestCode + "]");
      if (ContextCompat.checkSelfPermission(activity, permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission.GET_ACCOUNTS)) {
          // TODO do the entire permission request rigmarole properly.
          // Show an expanation to the user *asynchronously* -- don't block
          // this thread waiting for the user's response! After the user
          // sees the explanation, try again to request the permission.

        } else {

          // No explanation needed, we can request the permission.

          ActivityCompat.requestPermissions(activity,
                                            new String[]{permission.GET_ACCOUNTS},
                                            MY_PERMISSIONS_REQUEST_GET_ACCOUNTS);

          // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
          // app-defined int constant. The callback method gets the
          // result of the request.
        }
        return null;
      }

      final AccountManager accountManager = AccountManager.get(activity);
      if (!hasAuthenticator(accountManager)) {
        if (downloadRequestCode>=0) {
          startDownloadDialog(activity, downloadRequestCode);
        } else {
          activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
              Toast.makeText(activity, R.string.error_authenticator_not_installed, Toast.LENGTH_LONG).show();
            }
          });
        }
        return null;
      }
      String accountName = getStoredAccountName(activity);
      if (accountName!=null && source !=null) {
        Account account = new Account(accountName, ACCOUNT_TYPE);
        try {
          AccountManagerFuture<Bundle> resultFuture = accountManager.getAuthToken(account, ACCOUNT_TOKEN_TYPE, true, null, null);
          Bundle result = resultFuture.getResult();
          if (result!=null) {
            if (result.containsKey(AccountManager.KEY_INTENT)) {
              Intent intent = result.getParcelable(AccountManager.KEY_INTENT);
              Log.v(TAG, "Received AccountManager result with KEY_INTENT "+intent.getPackage());
              activity.startActivity(intent);
              return null;
            }
            Log.v(TAG, "Received AccountManager result without KEY_INTENT");
            if (accountManager.hasFeatures(account, new String[]{source.toString()}, null, null).getResult()) {
              return account;
            }
          }
        } catch (SecurityException|OperationCanceledException | IOException | AuthenticatorException e) {
          Log.w(TAG, "ensureAccount: ", e); // just throw the exception, there is another approach.
        }
      }
      final Account[] accounts = getCandidateAccounts(accountManager, activity, source);
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
      if (chooseRequestCode>=0) {
        // We didn't find the account we knew about
        @SuppressWarnings("deprecation")
        Intent intent = AccountManager.newChooseAccountIntent(null, null, new String[]{ACCOUNT_TYPE}, false, null, null, null, options);
        activity.startActivityForResult(intent, chooseRequestCode);
      } else {
        final Bundle result;
        try {
          result = accountManager.addAccount(ACCOUNT_TYPE, ACCOUNT_TOKEN_TYPE, null, options, activity, null, null).getResult();
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
