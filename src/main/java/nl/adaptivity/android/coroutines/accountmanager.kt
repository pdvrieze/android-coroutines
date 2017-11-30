@file:JvmName("AccountManagerUtil")
package nl.adaptivity.android.coroutines

import android.accounts.*
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import kotlinx.coroutines.experimental.CancellableContinuation
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.suspendCancellableCoroutine

// TODO This class is far from complete. Various account manager operations could be added.

/**
 * Get an authentication token from the account manager asynchronously. If required it will
 * take care of launching the permissions dialogs as needed.
 *
 * @see [AccountManager.getAuthToken]
 */
suspend fun <A: Activity> AccountManager.getAuthToken(activity: A, account: Account, authTokenType:String, options: Bundle? = null): String? {
    return suspendCancellableCoroutine<String?> { cont ->
        val callback = AccountManagerCallback<Bundle> { future: AccountManagerFuture<Bundle> ->
            if (future.isCancelled) {
                cont.cancel()
            } else {
                val resultBundle:Bundle = try {
                    future.result
                } catch (e: Exception) { // on any other exception just fail
                    if (e is OperationCanceledException) cont.resume(null) else cont.tryResumeWithException(e)
                    return@AccountManagerCallback
                }
                if (resultBundle.containsKey(AccountManager.KEY_INTENT)) {
                    val intent = resultBundle.get(AccountManager.KEY_INTENT) as Intent
                    activity.withActivityResult(intent) { activityResult ->
                        when (activityResult) {
                            is Maybe.Cancelled -> cont.cancel()
                            is Maybe.Ok -> async { cont.resume(getAuthToken(activity, account, authTokenType, options)) }
                        }
                    }
                    return@AccountManagerCallback
                } else {
                    cont.resume(resultBundle.getString(AccountManager.KEY_AUTHTOKEN))
                }
            }
        }

        getAuthToken(account, authTokenType, options, false, callback, null)
    }
}

private class CoroutineAccountManagerCallback<T>(private val cont: CancellableContinuation<T>): AccountManagerCallback<T> {
    override fun run(future: AccountManagerFuture<T>) {
        try {
            if (future.isCancelled) {
                cont.cancel()
            } else {
                cont.resume(future.result)
            }
        } catch (e: Exception) {
            if (e is CancellationException) {
                cont.cancel(e)
            } else {
                cont.tryResumeWithException(e)
            }
        }
    }
}

private suspend inline fun <R> AccountManager.callAsync(crossinline operation: AccountManager.(CoroutineAccountManagerCallback<R>) -> Unit): R {
    return suspendCancellableCoroutine<R> { cont ->
        operation(this@callAsync, CoroutineAccountManagerCallback(cont))
    }
}

suspend fun AccountManager.hasFeatures(account: Account, features: Array<String?>):Boolean {
    return callAsync { callback -> hasFeatures(account, features, callback, null) }
}
