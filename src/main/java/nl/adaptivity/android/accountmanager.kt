@file:JvmName("AccountManagerUtil")
@file:Suppress("PackageDirectoryMismatch")

package nl.adaptivity.android.accountmanager

import android.accounts.*
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.suspendCancellableCoroutine
import nl.adaptivity.android.coroutines.ActivityResult
import nl.adaptivity.android.coroutines.withActivityResult
import nl.adaptivity.android.kotlin.getValue
import nl.adaptivity.android.kotlin.weakRef

val Intent.accountName get() = getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
val Intent.accountType get() = getStringExtra(AccountManager.KEY_ACCOUNT_TYPE)
val Bundle.intent get() = get(AccountManager.KEY_INTENT) as Intent

suspend fun <A: Activity> AccountManager.getAuthToken(activity: A, account: Account, authTokenType:String, options: Bundle? = null, restart: A.() -> Unit): String? {
    val activity: A? by activity.weakRef
    return suspendCancellableCoroutine<String?> { cont ->
        val callback = AccountManagerCallback<Bundle> { future: AccountManagerFuture<Bundle> ->
            if (future.isCancelled) {
                cont.cancel()
            } else {
                val resultBundle:Bundle = try {
                    future.result
                } catch (e: OperationCanceledException) {
                    cont.resume(null) // No token, return null
                    return@AccountManagerCallback
                } catch (e: Exception) { // on any other exception just fail
                    cont.tryResumeWithException(e)
                    return@AccountManagerCallback
                }
                if (resultBundle.containsKey(AccountManager.KEY_INTENT)) {
                    val intent = resultBundle.intent
                    activity?.withActivityResult(intent) { activityResult ->
                        when (activityResult) {
                            is ActivityResult.Cancelled -> cont.cancel()
                            is ActivityResult.Ok -> cont.resume(runBlocking { getAuthToken(activity, account, authTokenType, options, restart) })
                        }
                    }
                    return@AccountManagerCallback
                } else {
                    cont.resume(resultBundle.getString(AccountManager.KEY_AUTHTOKEN))
                }
            }
        }

        getAuthToken(account, authTokenType, options, false, callback, null)

        TODO("implement")
    }
}