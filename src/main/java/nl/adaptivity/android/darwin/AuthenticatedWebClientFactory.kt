package nl.adaptivity.android.darwin

import android.Manifest
import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AuthenticatorException
import android.accounts.OperationCanceledException
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.annotation.RequiresPermission
import android.support.annotation.UiThread
import android.support.annotation.WorkerThread
import android.support.v4.content.ContextCompat
import android.util.Log
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async

import java.io.IOException
import java.net.URI

import nl.adaptivity.android.darwinlib.R

/**
 * A class for creating authenticated web clients
 */
object AuthenticatedWebClientFactory {

    private val TAG = "AuthWebClientFty"

    private val DEFAULT_AUTHBASE_ARRAY = arrayOf<String?>(null)

    private class ShowAccountTask(context: Context, private val authBase: URI) : AsyncTask<AuthenticatedWebClientCallbacks, Void, Runnable>() {
        /* Application context to avoid leaks. */
        @SuppressLint("StaticFieldLeak")
        private val context = context.applicationContext

        override fun doInBackground(vararg params: AuthenticatedWebClientCallbacks): Runnable {
            // TODO implement with coroutines
            val callbacks = params[0]
            val am = AccountManager.get(context)

            if (!hasAuthenticator(am)) return Runnable { callbacks.showDownloadDialog() }

            var currentAccount = getStoredAccount(context)

            if (!isAccountValid(context, am, currentAccount, authBase)) {
                currentAccount = null
            }

            val selectAccountIntent = selectAccount(context, currentAccount, authBase)
            return Runnable { callbacks.startSelectAccountActivity(selectAccountIntent) }
        }

        override fun onPostExecute(runnable: Runnable) {
            runnable.run()
        }
    }

    interface AuthenticatedWebClientCallbacks {
        /**
         * The system should present a download dialog. Probably using [DownloadDialog]
         */
        fun showDownloadDialog()

        /**
         * Called when an account needs to be selected. The receiver should normally call
         * [Activity.startActivity] with [selectAccount] as parameter.
         */
        fun startSelectAccountActivity(selectAccount: Intent)

        /**
         * Called when download was cancelled.
         */
        fun onDownloadCancelled()
    }

    /**
     * Get an authentication token. Just a forward to the actual client.
     */
    @Suppress("unused")
    @JvmStatic
    @WorkerThread
    fun getAuthToken(context: Context, authBase: URI, account: Account): String? {
        return AuthenticatedWebClientV14.getAuthToken(context, authBase, account)
    }

    @Suppress("unused")
    @JvmStatic
    fun getAuthBase(mBase: URI?): URI? {
        return mBase?.resolve("/accounts/")
    }

    internal fun getSharedPreferences(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    @Suppress("unused")
    @JvmStatic
    fun getStoredAccount(context: Context): Account? {
        val preferences = getSharedPreferences(context)
        val accountName = preferences.getString(AuthenticatedWebClient.KEY_ACCOUNT_NAME, null)
        return if (accountName == null) null else Account(accountName, AuthenticatedWebClient.ACCOUNT_TYPE)
    }

    @Suppress("unused")
    @JvmStatic
    fun setStoredAccount(context: Context, account: Account?) {
        val preferences = getSharedPreferences(context)
        if (account == null) {
            preferences.edit().remove(AuthenticatedWebClient.KEY_ACCOUNT_NAME).apply()
        } else {
            preferences.edit().putString(AuthenticatedWebClient.KEY_ACCOUNT_NAME, account.name).apply()
        }
    }

    @Suppress("unused")
    @WorkerThread
    @JvmStatic
    fun isAccountValid(context: Context, account: Account, authBase: URI?): Boolean {
        val accountManager = AccountManager.get(context)
        return isAccountValid(context, accountManager, account, authBase)
    }

    @SuppressLint("MissingPermission")
    @WorkerThread
    @JvmStatic
    fun isAccountValid(context: Context, accountManager: AccountManager, account: Account?, authBase: URI?): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.GET_ACCOUNTS) == PackageManager.PERMISSION_GRANTED) {
            return isAccountValid(accountManager, account, authBase)
        } else { // If we are not able to enumerate the accounts use a workaround

            val result = accountManager.getAuthToken(account, AuthenticatedWebClient.ACCOUNT_TOKEN_TYPE, null, false, null, null)

            try {
                result.result
            } catch (e: IllegalArgumentException) {
                return false
            } catch (e: AuthenticatorException) {
                Log.e(TAG, "Error logging in: ", e)
                return false
            } catch (e: IOException) {
                Log.e(TAG, "Error logging in: ", e)
                return false
            } catch (e: OperationCanceledException) {
                Log.e(TAG, "Error logging in: ", e)
                return false
            }

            return true
        }
    }

    @RequiresPermission(Manifest.permission.GET_ACCOUNTS)
    @WorkerThread
    @JvmStatic
    fun isAccountValid(am: AccountManager, account: Account?, authBase: URI?): Boolean {
        if (account == null) return false

        try {
            val future = am.hasFeatures(account, accountFeatures(authBase), null, null)
            return future.result
        } catch (e: SecurityException) {
            throw RuntimeException(e)
        } catch (e: OperationCanceledException) {
            throw RuntimeException(e)
        } catch (e: IOException) {
            throw RuntimeException(e)
        } catch (e: AuthenticatorException) {
            return false
        }

    }

    @JvmStatic
    fun hasAuthenticator(context: Context): Boolean {
        return hasAuthenticator(AccountManager.get(context))
    }

    @JvmStatic
    fun hasAuthenticator(am: AccountManager): Boolean {
        for (descriptor in am.authenticatorTypes) {
            if (AuthenticatedWebClient.ACCOUNT_TYPE == descriptor.type) {
                return true
            }
        }
        return false
    }


    internal fun accountFeatures(authbase: URI?): Array<String?> {
        return if (authbase == null) {
            DEFAULT_AUTHBASE_ARRAY
        } else arrayOf(authbase.toASCIIString())
    }

    @JvmStatic
    fun selectAccount(context: Context, account: Account?, authbase: URI?): Intent {
        val options: Bundle?
        if (authbase == null) {
            options = null
        } else {
            options = Bundle(1).apply { putString(AuthenticatedWebClient.KEY_AUTH_BASE, authbase.toASCIIString()) }
        }

        @Suppress("DEPRECATION")
        return AccountManager.newChooseAccountIntent(account, null, arrayOf(AuthenticatedWebClient.ACCOUNT_TYPE), false, context.getString(R.string.descriptionOverrideText), null, null, options)
    }

    @JvmStatic
    fun handleSelectAcountActivityResult(context: Context, resultCode: Int, resultData: Intent): Account? {
        if (resultCode == Activity.RESULT_OK) {
            val accountName = resultData.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            val accountType = resultData.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE)
            val account = Account(accountName, accountType)
            setStoredAccount(context, account)
            return account
        } // else ignore
        return getStoredAccount(context) // Just return the stored account instead.
    }

    /**
     * Function that is called when the authenticator install intent returns. Instead of looking at
     * the results, just check that the application is now installed.
     * @param context The context to use
     * @param resultCode The result code from the calling activity
     * @param resultData The data linked to the activity call result.
     *
     * @return `true` if installed, `false` if not.
     */
    @Suppress("UNUSED_PARAMETER")
    @JvmStatic
    fun handleInstallAuthenticatorActivityResult(context: Context, resultCode: Int, resultData: Intent?): Boolean {
        val pm = context.packageManager
        try {
            pm.getPackageInfo("uk.ac.bournemouth.darwin.auth", 0)
            // throws if the package is not installed
            return true
        } catch (e: PackageManager.NameNotFoundException) {
            return false
        }

    }

    suspend fun ensureAuthenticator(context: AuthenticationContext): Boolean {
        if (hasAuthenticator(context.context)) return true
        val dialog = SuspDownloadDialog.newInstance(context.downloadRequestCode)
        return dialog.show(context.activity, context.downloadRequestCode) is DialogResult.Success
    }

    suspend fun ensureAccount(context: AuthenticationContext, authBase: URI?) : Account? {
        // If we have a stored, valid, account, just return it
        getStoredAccount(context.context)?.let { account ->
            if (isAccountValid(context.context, account, authBase)) {
                return account
            } else { // Not valid, forget the account
                setStoredAccount(context.context, null)
            }
        }
        if (!ensureAuthenticator(context)) return null
        TODO("Rest still needed")
    }

    @WorkerThread
    @JvmStatic
    fun tryEnsureAccount(context: Context, authBase: URI?, ensureCallbacks: AuthenticatedWebClientCallbacks): Account? {
        run {
            val account = getStoredAccount(context) // Get the stored account, if we have one check that it is valid
            if (account != null) {
                if (isAccountValid(context, account, authBase)) {
                    return account
                }
                setStoredAccount(context, null)
            }
        }
        if (!hasAuthenticator(context)) {
            ensureCallbacks.showDownloadDialog()
            return null
        }
        val selectAccount = selectAccount(context, null, authBase)
        ensureCallbacks.startSelectAccountActivity(selectAccount)

        return null
    }

    @Suppress("unused")
    @UiThread
    @JvmStatic
    fun showAccountSelection(activity: Activity, callbacks: AuthenticatedWebClientCallbacks, authBase: URI) {
        ShowAccountTask(activity, authBase).execute(callbacks)
        getStoredAccount(activity)
    }

    @JvmStatic
    fun doShowDownloadDialog(activity: Activity, requestCode: Int) {
        val dialog = DownloadDialog.newInstance(requestCode)
        dialog.show(activity.fragmentManager, AuthenticatedWebClient.DOWNLOAD_DIALOG_TAG)
    }



    @JvmStatic
    fun newClient(context: Context, account: Account, authbase: URI?): AuthenticatedWebClient {
        return AuthenticatedWebClientV14(context, account, authbase)
    }


    @JvmStatic
    fun <R> withClient(context: Context, account: Account, authBase: URI?, body: suspend (AuthenticatedWebClient)->R):Deferred<R> {
        return async {

            val client = newClientAsync(context, account, authBase)

            if (isActive) body(client) else throw CancellationException()
        }
    }

    private fun newClientAsync(context: Context, account: Account, authBase: URI?): AuthenticatedWebClient {
        return AuthenticatedWebClientV14(context, account, authBase)
    }

}
