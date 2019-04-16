@file:JvmName("AccountManagerUtil")

package nl.adaptivity.android.coroutines

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AccountManagerCallback
import android.accounts.AccountManagerFuture
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.annotation.RequiresPermission
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

// TODO This class is far from complete. Various account manager operations could be added.

/**
 * Get an authentication token from the account manager asynchronously. If required it will
 * take care of launching the permissions dialogs as needed.
 *
 * @see [AccountManager.getAuthToken]
 */
@RequiresPermission("android.permission.USE_CREDENTIALS")
@Deprecated("Use the safer one that takes an ActivityCoroutineScope")
suspend fun <A : Activity> AccountManager.getAuthToken(activity: A, account: Account, authTokenType: String, options: Bundle? = null): String? {
    @Suppress("DEPRECATION")
    val resultBundle = callAsync<Bundle> { callback -> getAuthToken(account, authTokenType, options, false, callback, null) }
    if (resultBundle.containsKey(AccountManager.KEY_INTENT)) {
        val intent = resultBundle.get(AccountManager.KEY_INTENT) as Intent
        val activityResult = activity.activityResult(intent)
        @Suppress("DEPRECATION")
        return activityResult.onOk { AccountManager.get(activity).getAuthToken(activity, account, authTokenType, options) }
    } else {
        return resultBundle.getString(AccountManager.KEY_AUTHTOKEN)
    }
}

/**
 * Get an authentication token from the account manager asynchronously. If required it will
 * take care of launching the permissions dialogs as needed.
 *
 * @see [AccountManager.getAuthToken]
 */
@RequiresPermission("android.permission.USE_CREDENTIALS")
suspend fun <A : Activity> LayoutContainerCoroutineScope<A, *>.getAuthToken(account: Account, authTokenType: String, options: Bundle? = null): String? {
    val resultBundle = callAccountManagerAsync<Bundle> { callback -> getAuthToken(account, authTokenType, options, false, callback, null) }
    if (resultBundle.containsKey(AccountManager.KEY_INTENT)) {
        val intent = resultBundle.get(AccountManager.KEY_INTENT) as Intent
        val activityResult = activity().activityResult(intent)
        return activityResult.onOk { getAuthToken(account, authTokenType, options) }
    } else {
        return resultBundle.getString(AccountManager.KEY_AUTHTOKEN)
    }
}

/**
 * Callback class that uses a continuation as the callback for the account manager. Note that
 * this callback is NOT designed to survive the destruction of the [Context] ([Activity]).
 *
 * @property cont The continuation that will be invoked on completion.
 */
class CoroutineAccountManagerCallback<T>(private val cont: CancellableContinuation<T>) : AccountManagerCallback<T> {
    @UseExperimental(InternalCoroutinesApi::class)
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

/**
 * Helper function that helps with calling account manager operations asynchronously.
 *
 * @receiver The account manager. This is actually not stable.
 */
@Deprecated("Use a special ContextCoroutine that doesn't put a context in the capture", ReplaceWith("callAccountManagerAsync(context, operation)"))
suspend inline fun <R> AccountManager.callAsync(crossinline operation: AccountManager?.(CoroutineAccountManagerCallback<R>) -> Unit): R {
    return suspendCancellableCoroutine<R> { cont ->
        operation(CoroutineAccountManagerCallback(cont))
    }
}

/**
 * Helper function that helps with calling account manager operations asynchronously.
 */
suspend inline fun <R> ContextedCoroutineScope<*,*>.callAccountManagerAsync(crossinline operation: AccountManager.(CoroutineAccountManagerCallback<R>) -> Unit): R {
    val androidContext = getAndroidContext()
    return suspendCancellableCoroutine { cont ->
        AccountManager.get(androidContext).operation(CoroutineAccountManagerCallback(cont))
    }
}

/**
 * Determine whether the account manager has the given features. This is the suspending equivalent of
 * [AccountManager.hasFeatures].
 *
 * @see [AccountManager.hasFeatures].
 */
@Suppress("DEPRECATION")
@Deprecated("Use ActivityCoroutineScope version")
suspend fun AccountManager.hasFeatures(account: Account, features: Array<String?>): Boolean {
    return callAsync { callback -> hasFeatures(account, features, callback, null) }
}

/**
 * Determine whether the account manager has the given features. This is the suspending equivalent of
 * [AccountManager.hasFeatures].
 *
 * @see [AccountManager.hasFeatures].
 */
internal suspend fun ContextedCoroutineScope<*,*>.accountHasFeaturesImpl(account: Account, features: Array<String?>): Boolean {
    return callAccountManagerAsync { callback -> hasFeatures(account, features, callback, null) }
}
