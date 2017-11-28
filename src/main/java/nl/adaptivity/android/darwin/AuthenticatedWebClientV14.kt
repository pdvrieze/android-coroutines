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

import android.accounts.*
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.os.Build
import android.support.annotation.WorkerThread
import android.util.Log
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch
import nl.adaptivity.android.accountmanager.getAuthToken
import java.io.IOException
import java.net.*


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

    suspend fun execute(activity: Activity, request: AuthenticatedWebClient.WebRequest): HttpURLConnection? {
        val am = AccountManager.get(activity)
        val token = am.getAuthToken(activity, account, AuthenticatedWebClient.ACCOUNT_TOKEN_TYPE)
        if (token!=null) {
            val cookieUri = request.uri
            val cookieStore = cookieManager.cookieStore

            for(repeat in 0..1) {
                val cookie = HttpCookie(AuthenticatedWebClient.DARWIN_AUTH_COOKIE, token)

                if ("https" == request.uri.scheme.toLowerCase()) {
                    cookie.secure = true
                }
                removeConflictingCookies(cookieStore, cookie)
                cookieStore.add(cookieUri, cookie)
                request.setHeader(AuthenticatedWebClient.DARWIN_AUTH_COOKIE, token)

                val connection = request.connection
                when {
                    connection.responseCode == HttpURLConnection.HTTP_UNAUTHORIZED && repeat==0 -> {
                        try {
                            connection.errorStream.use { errorStream ->
                                errorStream.skip(Integer.MAX_VALUE.toLong())
                            }
                        } finally {
                            connection.disconnect()
                        }

                        Log.d(TAG, "execute: Invalidating auth token")
                        am.invalidateAuthToken(AuthenticatedWebClient.ACCOUNT_TYPE, token)
                    }
                    else -> return connection
                }
            }

        }
        return null
    }

    override fun Activity.execute(request: AuthenticatedWebClient.WebRequest, currentlyInRetry: Boolean, onError: (HttpURLConnection?) -> Unit, onSuccess: (HttpURLConnection)->Unit): Job {
        return launch {
            val connection = execute(this@execute, request)
            when {
                connection==null -> onError(null)
                connection.responseCode in 200..399 -> onSuccess(connection)
                else -> onError(connection)
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
        val cookieStore = cookieManager.cookieStore
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

        return AuthenticatedWebClientFactory.getAuthToken(accountManager, context, this.authBase, this.account)

    }

    companion object {

        private val TAG = AuthenticatedWebClientV14::class.java.name

    }

}