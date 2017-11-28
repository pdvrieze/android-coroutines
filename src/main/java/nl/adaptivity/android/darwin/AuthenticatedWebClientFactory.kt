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
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.annotation.RequiresPermission
import android.support.annotation.WorkerThread
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.util.Log
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.NonCancellable.isActive
import nl.adaptivity.android.accountmanager.account
import nl.adaptivity.android.accountmanager.accountName
import nl.adaptivity.android.accountmanager.accountType
import nl.adaptivity.android.accountmanager.hasFeatures
import nl.adaptivity.android.coroutines.*
import nl.adaptivity.android.darwinlib.R
import java.io.File
import java.io.IOException
import java.net.URI

/**
 * A class for creating authenticated web clients
 */
object AuthenticatedWebClientFactory {

    private val TAG = "AuthWebClientFty"

    private val DEFAULT_AUTHBASE_ARRAY = arrayOf<String?>(null)

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
            } catch (e: Exception) {
                when (e) {
                    is IllegalArgumentException,
                    is IOException,
                    is OperationCanceledException,
                    is AuthenticatorException -> {
                        Log.e(TAG, "Error logging in: ", e)
                        return false
                    }
                    else -> throw e
                }
            }

            return true
        }
    }

    @RequiresPermission(Manifest.permission.GET_ACCOUNTS)
    @WorkerThread
    @JvmStatic
    @Deprecated("Use suspend version", ReplaceWith("am.isAccountValid(account, authBase)"))
    fun isAccountValid(am: AccountManager, account: Account?, authBase: URI?): Boolean {
        if (account == null) return false

        try {
            return am.hasFeatures(account, AuthenticatedWebClientFactory.accountFeatures(authBase), null, null).result
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
        return am.authenticatorTypes.any { AuthenticatedWebClient.ACCOUNT_TYPE == it.type }
    }


    internal fun accountFeatures(authbase: URI?): Array<String?> =
            authbase?.let { arrayOf(it.toASCIIString()) } ?: DEFAULT_AUTHBASE_ARRAY

    @JvmStatic
    fun selectAccount(context: Context, account: Account?, authbase: URI?): Intent {
        val options = authbase?.let { Bundle(1).apply { putString(AuthenticatedWebClient.KEY_AUTH_BASE, it.toASCIIString()) } }

        @Suppress("DEPRECATION")
        return AccountManager.newChooseAccountIntent(account,
                null,
                arrayOf(AuthenticatedWebClient.ACCOUNT_TYPE),
                false,
                context.getString(R.string.descriptionOverrideText),
                null,
                null, options)
    }

    @JvmStatic
    fun handleSelectAcountActivityResult(context: Context, resultCode: Int, resultData: Intent): Account? {
        if (resultCode == Activity.RESULT_OK) {
            return with(resultData) { Account(accountName, accountType) }.also { account ->
                setStoredAccount(context, account)
            }
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

    suspend fun ensureAuthenticator(activity: Activity): Boolean {
        if (hasAuthenticator(activity)) return true
        return tryDownloadAndInstallAuthenticator(activity) is Maybe.Ok
    }

    suspend fun ensureAccount(activity: Activity, authBase: URI?) : Maybe<Account?> {
        // If we have a stored, valid, account, just return it
        getStoredAccount(activity)?.let { account ->
            if (isAccountValid(activity, account, authBase)) {
                return Maybe.Ok(account)
            } else { // Not valid, forget the account
                setStoredAccount(activity, null)
            }
        }
        if (!ensureAuthenticator(activity)) return Maybe.cancelled()
        return activity.activityResult(selectAccount(activity, null, authBase)).map { it?.account?.also { setStoredAccount(activity, it) } }
    }

    @JvmStatic
    fun tryEnsureAccount(context: Activity, authBase: URI?, callback: SerializableHandler<Activity, Maybe<Account?>>): Account? {
        async(start = CoroutineStart.UNDISPATCHED) {
            callback(context, ensureAccount(context, authBase))
        }
/*

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
*/

        return null
    }

    @JvmStatic
    fun tryDownloadAndInstallAuthenticator(activity: Activity, handler: SerializableHandler<Activity, ActivityResult>) {
        launch {
            handler(activity, tryDownloadAndInstallAuthenticator(activity))
        }
    }

    @JvmStatic
    fun doShowDownloadDialog(activity: Activity, handler: SerializableHandler<Activity, ActivityResult>) {
        return tryDownloadAndInstallAuthenticator(activity, handler)
    }

    suspend fun tryDownloadAndInstallAuthenticator(activity: Activity): ActivityResult {
        if (SuspDownloadDialog.newInstance(-1).show(activity, AuthenticatedWebClient.DOWNLOAD_DIALOG_TAG).flatMap() != true) {
            return Maybe.cancelled()
        }
        if (!isActive) return Maybe.cancelled()

        val downloadedApk = DownloadFragment.download(activity, Uri.parse(AUTHENTICATOR_URL))
        if (!isActive) return Maybe.cancelled()

        val downloaded = File(downloadedApk)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            doInstall(activity, FileProvider.getUriForFile(activity, "${activity.applicationInfo.packageName}.darwinlib.fileProvider", downloaded))
        } else {
            doInstall(activity, Uri.fromFile(downloaded))
        }
    }



    suspend fun doInstall(activity: Activity, uri: Uri): ActivityResult {
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return activity.activityResult(installIntent)
    }



    @JvmStatic
    fun newClient(context: Context, account: Account, authbase: URI?): AuthenticatedWebClient {
        return AuthenticatedWebClientV14(account, authbase)
    }


    @JvmStatic
    fun <R> withClient(context: Context, account: Account, authBase: URI?, body: suspend (AuthenticatedWebClient)->R):Deferred<R> {
        return async {

            val client = newClientAsync(context, account, authBase)

            if (isActive) body(client) else throw CancellationException()
        }
    }

    private fun newClientAsync(context: Context, account: Account, authBase: URI?): AuthenticatedWebClient {
        return AuthenticatedWebClientV14(account, authBase)
    }

}


suspend fun AccountManager.isAccountValid(account: Account?, authBase: URI?): Boolean {
    if (account == null) return false

    try {
        return hasFeatures(account, AuthenticatedWebClientFactory.accountFeatures(authBase))
    } catch (e: AuthenticatorException) {
        return false
    }
}

internal const val AUTHENTICATOR_URL = "https://darwin.bournemouth.ac.uk/darwin-auth.apk"
