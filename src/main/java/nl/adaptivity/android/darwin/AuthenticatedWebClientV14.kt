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

package nl.adaptivity.android.darwin

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AccountManagerCallback
import android.accounts.AccountManagerFuture
import android.accounts.AuthenticatorException
import android.accounts.OperationCanceledException
import android.annotation.TargetApi
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.annotation.WorkerThread
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.util.Log
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch
import nl.adaptivity.android.accountmanager.getAuthToken

import java.io.IOException
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.CookieStore
import java.net.HttpCookie
import java.net.HttpURLConnection
import java.net.URI

import nl.adaptivity.android.darwinlib.R


/**
 * A class making it easier to make authenticated requests to darwin.
 *
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
internal class AuthenticatedWebClientV14(override val account: Account, override val authBase: URI?) : AuthenticatedWebClient {

    private var token: String? = null
    private val cookieManager: CookieManager by lazy<CookieManager> {
        CookieManager(MyCookieStore(), CookiePolicy.ACCEPT_ORIGINAL_SERVER).also {
            CookieHandler.setDefault(it)
        }
    }

    @WorkerThread
    @Throws(IOException::class)
    override fun execute(context: Context, request: AuthenticatedWebClient.WebRequest): HttpURLConnection? {
        return execute(context, request, false)
    }

    //    @WorkerThread
    override fun Activity.execute(request: AuthenticatedWebClient.WebRequest, currentlyInRetry: Boolean, onError: (HttpURLConnection)->Unit, callback: (HttpURLConnection)->Unit): Job {
        val context = this.applicationContext
        return launch {
            val am = AccountManager.get(context)

            val token = am.getAuthToken(activity = this@execute, account = account, authTokenType = AuthenticatedWebClient.ACCOUNT_TOKEN_TYPE, restart ={ execute(request, false, onError, callback) })
            if (token!=null) {

                val cookieUri = request.uri
                //    cookieUri = cookieUri.resolve("/");

                val cookie = HttpCookie(AuthenticatedWebClient.DARWIN_AUTH_COOKIE, token)
                //    cookie.setDomain(cookieUri.getHost());
                //    cookie.setVersion(1);
                //    cookie.setPath("/");
                if ("https" == request.uri.scheme.toLowerCase()) {
                    cookie.secure = true
                }
                val cookieStore = cookieManager.cookieStore
                removeConflictingCookies(cookieStore, cookie)
                cookieStore.add(cookieUri, cookie)
                request.setHeader(AuthenticatedWebClient.DARWIN_AUTH_COOKIE, token)

                val connection = request.connection
                try {

                    when {
                        connection.responseCode in 200..399 -> callback(connection)
                        connection.responseCode == HttpURLConnection.HTTP_UNAUTHORIZED && !currentlyInRetry -> {
                            connection.errorStream.use { errorStream ->
                                errorStream.skip(Integer.MAX_VALUE.toLong())
                            }

                            Log.d(TAG, "execute: Invalidating auth token")
                            am.invalidateAuthToken(AuthenticatedWebClient.ACCOUNT_TYPE, token)
                            execute(request, true, onError, callback)
                        }
                        else -> onError(connection)
                    }
                } finally { // Catch exception as there would be no way for a caller to disconnect
                    connection.disconnect()
                }

            }
        }

    }

    @WorkerThread
    @Throws(IOException::class)
    private fun execute(context: Context, request: AuthenticatedWebClient.WebRequest, currentlyInRetry: Boolean): HttpURLConnection? {
        Log.d(TAG, "execute() called with: request = [$request], currentlyInRetry = [$currentlyInRetry]")
        val accountManager = AccountManager.get(context)
        token = getAuthToken(context, accountManager)
        if (token == null) {
            return null
        }

        val cookieUri = request.uri
        //    cookieUri = cookieUri.resolve("/");

        val cookie = HttpCookie(AuthenticatedWebClient.DARWIN_AUTH_COOKIE, token)
        //    cookie.setDomain(cookieUri.getHost());
        //    cookie.setVersion(1);
        //    cookie.setPath("/");
        if ("https" == request.uri.scheme.toLowerCase()) {
            cookie.secure = true
        }
        val cookieStore = cookieManager!!.cookieStore
        removeConflictingCookies(cookieStore, cookie)
        cookieStore.add(cookieUri, cookie)
        request.setHeader(AuthenticatedWebClient.DARWIN_AUTH_COOKIE, token!!)

        val connection = request.connection
        try {


            if (connection.responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                connection.errorStream.use { errorStream ->
                    errorStream.skip(Integer.MAX_VALUE.toLong())
                }

                Log.d(TAG, "execute: Invalidating auth token")
                accountManager.invalidateAuthToken(AuthenticatedWebClient.ACCOUNT_TYPE, token)
                if (!currentlyInRetry) { // Do not repeat retry
                    return execute(context, request, true)
                }
            }
            return connection
        } catch (e: Throwable) { // Catch exception as there would be no way for a caller to disconnect
            connection.disconnect()
            throw e
        }

    }

    private fun removeConflictingCookies(cookieStore: CookieStore, refCookie: HttpCookie) {
        val toRemove = cookieStore.cookies.filter { it.name == refCookie.name }
        for (c in toRemove) {
            for (url in cookieStore.urIs) {
                cookieStore.remove(url, c)
            }
        }
    }

    @WorkerThread
    private fun getAuthToken(context: Context, accountManager: AccountManager): String? {
        if (token != null) return token

        return getAuthToken(accountManager, context, this.authBase, this.account)

    }

    companion object {

        private val TAG = AuthenticatedWebClientV14::class.java.name

        @WorkerThread
        fun getAuthToken(context: Context, authBase: URI, account: Account): String? {
            return getAuthToken(AccountManager.get(context), context, authBase, account)
        }

        @WorkerThread
        fun getAuthToken(accountManager: AccountManager, context: Context, authBase: URI?, account: Account): String? {
            if (!AuthenticatedWebClientFactory.isAccountValid(context, accountManager, account, authBase)) {
                throw AuthenticatedWebClient.InvalidAccountException()
            }

            val callback = AccountManagerCallback<Bundle> { future ->
                try {
                    val bundle = future.result
                    if (bundle.containsKey(AccountManager.KEY_INTENT)) {
                        val intent = bundle.get(AccountManager.KEY_INTENT) as Intent
                        if (context is Activity) {
                            context.startActivity(intent)
                        } else {
                            val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
                            val contentInfo = context.getString(R.string.notification_authreq_contentInfo)
                            val actionLabel = context.getString(R.string.notification_authreq_action)
                            val notificationManager = NotificationManagerCompat.from(context)
                            val channel = context.notificationChannel
                            val notification = NotificationCompat.Builder(context, channel).setSmallIcon(R.drawable.ic_notification)
                                    .setContentInfo(contentInfo)
                                    .setContentIntent(pendingIntent)
                                    .addAction(R.drawable.ic_notification, actionLabel, pendingIntent)
                                    .build()
                            notificationManager.notify(0, notification)
                        }
                    }
                } catch (e: IllegalArgumentException) {
                    Log.w(TAG, "The requested account does not exist")
                    throw AuthenticatedWebClient.InvalidAccountException(e)
                } catch (e: OperationCanceledException) {
                    Log.d(TAG, "Failed to get account", e)
                } catch (e: AuthenticatorException) {
                    Log.d(TAG, "Failed to get account", e)
                } catch (e: IOException) {
                    Log.d(TAG, "Failed to get account", e)
                }
            }
            val result: AccountManagerFuture<Bundle>
            result = accountManager.getAuthToken(account, AuthenticatedWebClient.ACCOUNT_TOKEN_TYPE, null, false, callback, null)

            val bundle: Bundle
            try {
                bundle = result.result
            } catch (e: AuthenticatorException) {
                Log.e(TAG, "Error logging in: ", e)
                return null
            } catch (e: IOException) {
                Log.e(TAG, "Error logging in: ", e)
                return null
            } catch (e: OperationCanceledException) {
                Log.e(TAG, "Error logging in: ", e)
                return null
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "No such account: ", e)
                return null
            }

            return bundle.getString(AccountManager.KEY_AUTHTOKEN)
        }


        @Suppress("ObjectPropertyName")
        private var _notificationChannel: String? = null
        private val Context.notificationChannel: String get() {
            _notificationChannel?.let { return it }
            val channelId = "AuthWebClient"
            if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O) {
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (manager.getNotificationChannel(channelId)!=null) {
                    val channel = NotificationChannel(channelId, "Darwin Authentication", NotificationManager.IMPORTANCE_HIGH).apply {
                        this.setShowBadge(true)
                    }
                    manager.createNotificationChannel(channel)
                }
            }
            return channelId.also { _notificationChannel = it }
        }
    }

}