package nl.adaptivity.android.coroutines

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import kotlinx.coroutines.suspendCancellableCoroutine
import nl.adaptivity.android.util.GrantResult
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.resume

/**
 * Extension method for activity that invokes [Activity.startActivityForResult] and invokes the
 * callback in [body] when complete. For use in Kotlin consider [activityResult] as a suspending
 * function instead.
 *
 * @receiver The activity that is extended.
 * @param intent The intent to invoke.
 * @param body The callback invoked on completion.
 */
fun <A:Activity> A.withActivityResult(intent: Intent, body: A.(ActivityResult)->Unit) {
    // Horrible hack to fix generics
    @Suppress("UNCHECKED_CAST")
    val contFragment = RetainedContinuationFragment(ParcelableContinuation(body, COROUTINEFRAGMENT_RESULTCODE_START))
    fragmentManager.beginTransaction().add(contFragment, RetainedContinuationFragment.TAG).commit()
    runOnUiThread {
        fragmentManager.executePendingTransactions()
        contFragment.startActivityForResult(intent, COROUTINEFRAGMENT_RESULTCODE_START)
    }
}

/**
 * Asynchronously invoke [Activity.startActivityForResult] returning the result on completion.
 */
suspend fun Activity.activityResult(intent:Intent): ActivityResult {
    return suspendCoroutine { continuation ->
        val fm = fragmentManager
        val contFragment = RetainedContinuationFragment(ParcelableContinuation(continuation, this, COROUTINEFRAGMENT_RESULTCODE_START))

        fm.beginTransaction().apply {
            // This shouldn't happen, but in that case remove the old continuation.
            fm.findFragmentByTag(RetainedContinuationFragment.TAG)?.let { remove(it) }

            add(contFragment, RetainedContinuationFragment.TAG)
        }.commit()


        runOnUiThread {
            fm.executePendingTransactions()
            contFragment.startActivityForResult(intent, COROUTINEFRAGMENT_RESULTCODE_START)
        }
    }
}

/**
 * Suspending function to request permissions. To avoid a dependency on the support library it
 * reimplements the fallback behaviour for pre-marshmallow devices. On later devices the coroutine
 * will be retained across activity restarts.
 *
 * @receiver The activity to use for the request.
 * @param The permissions to request
 * @return Either null when the request was cancelled by the user or a data class wrapping the parameters of [Activity.onRequestPermissionsResult]
 */
suspend fun Activity.requestPermissions(permissions: Array<String>): GrantResult? {
    return suspendCancellableCoroutine { continuation ->

        runOnUiThread {
            if (Build.VERSION.SDK_INT < 23) {
                // TODO consider whether this should be "retained". Probably not.
                val pm = packageManager
                val packageName = packageName
                val grantResults = IntArray(permissions.size) { idx -> pm.checkPermission(permissions[idx], packageName) }
                continuation.resume(GrantResult(permissions, grantResults))
            } else {
                val fragment = RequestPermissionContinuationFragment(ParcelableContinuation(continuation, this, COROUTINEFRAGMENT_RESULTCODE_START))
                val fm = fragmentManager
                fm.beginTransaction().apply {
                    fm.findFragmentByTag(RequestPermissionContinuationFragment.TAG)?.let { remove(it) }
                    add(fragment, RequestPermissionContinuationFragment.TAG)
                }.commit()

                fm.executePendingTransactions()
                fragment.requestPermissions(permissions, COROUTINEFRAGMENT_RESULTCODE_START)
            }
        }
    }
}

/**
 * Extension method for activity that invokes [Activity.startActivityForResult] and invokes the
 * callback in [body] when complete. For use in Kotlin consider [activityResult] as a suspending
 * function instead.
 *
 * @receiver The activity that is extended.
 * @param intent The intent to invoke.
 * @param options The options to pass on the activity start.
 * @param body The callback invoked on completion.
 */
@Suppress("unused")
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
fun <A: Activity> A.withActivityResult(intent: Intent, options: Bundle?, body: SerializableHandler<A, ActivityResult>) {
    // Horrible hack to fix generics
    @Suppress("UNCHECKED_CAST")
    val contFragment = RetainedContinuationFragment(ParcelableContinuation(body, COROUTINEFRAGMENT_RESULTCODE_START))
    fragmentManager.beginTransaction().add(contFragment, RetainedContinuationFragment.TAG).commit()
    runOnUiThread {
        fragmentManager.executePendingTransactions()
        contFragment.startActivityForResult(intent, COROUTINEFRAGMENT_RESULTCODE_START, options)
    }
}

/**
 * The starting (and for now only) result code that is used to start the activity. As it happens
 * from a special fragment the result code is actually ignored and should be safe from conflict.
 */
const val COROUTINEFRAGMENT_RESULTCODE_START = 0xf00

/**
 * The [Bundle] key under which the continuation is stored.
 */
const val KEY_ACTIVITY_CONTINUATIONS_STATE = "parcelableContinuations"

/**
 * Java compatibility interface to make the asynchronous use of [withActivityResult] with a callback
 * much friendlier.
 */
interface SerializableHandler<A, T> {
    operator fun invoke(activty: A, data:T)
}

typealias ActivityResult = Maybe<Intent?>

