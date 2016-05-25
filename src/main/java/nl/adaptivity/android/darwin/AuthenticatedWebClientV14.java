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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;

import nl.adaptivity.android.darwinlib.R;


/**
 * A class making it easier to make authenticated requests to darwin.
 *
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
class AuthenticatedWebClientV14 implements AuthenticatedWebClient {

  private static final String TAG = AuthenticatedWebClientV14.class.getName();

  private final Context mContext;

  private String mToken = null;

  private final URI mAuthBase;

  private Account mAccount;
  private CookieManager mCookieManager;

  AuthenticatedWebClientV14(@NonNull final Context context, @NonNull final Account account, @Nullable final URI authBase) {
    if (account==null) { throw new NullPointerException(); }
    mContext = context;
    mAccount = account;
    mAuthBase = authBase;
  }

  @Override
  public URI getAuthBase() {
    return mAuthBase;
  }

  @Override
  public Account getAccount() {
    return mAccount;
  }

  @WorkerThread
  public HttpURLConnection execute(final WebRequest request) throws IOException {
    return execute(request, false);
  }

  @Nullable
  @WorkerThread
  private HttpURLConnection execute(final WebRequest request, final boolean currentlyInRetry) throws IOException {
    Log.d(TAG, "execute() called with: " + "request = [" + request + "], currentlyInRetry = [" + currentlyInRetry + "]");
    final AccountManager accountManager = AccountManager.get(mContext);
    mToken = getAuthToken(accountManager);
    if (mToken == null) { return null; }

    if (mCookieManager == null) {
      mCookieManager = new CookieManager(new MyCookieStore(), CookiePolicy.ACCEPT_ORIGINAL_SERVER);
      CookieHandler.setDefault(mCookieManager);
    }

    URI cookieUri = request.getUri();
//    cookieUri = cookieUri.resolve("/");

    HttpCookie cookie = new HttpCookie(DARWIN_AUTH_COOKIE, mToken);
//    cookie.setDomain(cookieUri.getHost());
//    cookie.setVersion(1);
//    cookie.setPath("/");
    if ("https".equals(request.getUri().getScheme().toLowerCase())) {
      cookie.setSecure(true);
    }
    CookieStore cookieStore = mCookieManager.getCookieStore();
    cookieStore.add(cookieUri, cookie);
    request.setHeader(DARWIN_AUTH_COOKIE, mToken);

    HttpURLConnection connection = request.getConnection();
    try {


      if (connection.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
        InputStream errorStream = connection.getErrorStream();
        try {
          errorStream.skip(Integer.MAX_VALUE);
        } finally {
          errorStream.close();
        }
        Log.d(TAG, "execute: Invalidating auth token");
        accountManager.invalidateAuthToken(AuthenticatedWebClient.ACCOUNT_TYPE, mToken);
        if (!currentlyInRetry) { // Do not repeat retry
          return execute(request, true);
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
    for (HttpCookie existingCookie : cookieStore.getCookies()) {
      if (existingCookie.getName().equals(refCookie.getName())) toRemove.add(existingCookie);
    }
    for (HttpCookie c : toRemove) {
      cookieStore.remove(null, c);
      for (URI url : cookieStore.getURIs()) {
        cookieStore.remove(url, c);
      }
    }
  }

  @WorkerThread
  private String getAuthToken(final AccountManager accountManager) {
    if (mToken != null) return mToken;
    if (! AuthenticatedWebClientFactory.isAccountValid(accountManager, mAccount, mAuthBase)) {
      throw new InvalidAccountException();
    }

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
              String contentInfo = mContext.getString(R.string.notification_authreq_contentInfo);
              String actionLabel = mContext.getString(R.string.notification_authreq_action);
              Notification notification = new NotificationCompat.Builder(mContext).setSmallIcon(R.drawable.ic_notification)
                                                                                  .setContentInfo(contentInfo)
                                                                                  .setContentIntent(pendingIntent)
                                                                                  .addAction(R.drawable.ic_notification, actionLabel, pendingIntent)
                                                                                  .build();
              NotificationManagerCompat.from(mContext).notify(0, notification);
            }
          }
        } catch (IllegalArgumentException e) {
          Log.w(TAG, "The requested account does not exist");
          throw new InvalidAccountException(e);
        } catch (OperationCanceledException | AuthenticatorException | IOException e) {
          Log.d(TAG, "Failed to get account", e);
        }
      }
    };
    final AccountManagerFuture<Bundle> result;
    result = accountManager.getAuthToken(mAccount, ACCOUNT_TOKEN_TYPE, null, false, callback, null);

    final Bundle bundle;
    try {
      bundle = result.getResult();
    } catch (AuthenticatorException|IOException|OperationCanceledException e) {
      Log.e(TAG, "Error logging in: ", e);
      return null;
    } catch (IllegalArgumentException e) {
      Log.e(TAG, "No such account: ", e);
      return null;
    }
    return bundle.getString(AccountManager.KEY_AUTHTOKEN);

  }

}