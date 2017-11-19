package nl.adaptivity.android.darwin

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.support.annotation.RequiresApi
import android.support.v4.app.SupportActivity
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.android.UI
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Activity class that has additional support for coroutines
 */
open class CoroutineActivity: SupportActivity() {
    private var activityContinuation: ParcelableContinuation<ActivityResult>? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(resultCode) {
            Activity.RESULT_OK -> activityContinuation?.resume(ActivityResult.Ok(data))
            Activity.RESULT_CANCELED -> activityContinuation?.resume(ActivityResult.Cancelled)
        }
    }

    suspend fun activityResult(intent: Intent): ActivityResult {
        startActivityForResult(intent, REQUEST_CODE_START)
        return suspendCancellableCoroutine<ActivityResult> { cont ->
            activityContinuation?.cancel(IllegalStateException("Starting new continuation while one already exists"))
            activityContinuation = cont
        }
    }

    @RequiresApi(16)
    suspend fun activityResult(intent: Intent, options: Bundle? = null): ActivityResult {
        startActivityForResult(intent, REQUEST_CODE_START, options)
        return suspendCancellableCoroutine<ActivityResult> { cont ->
            activityContinuation?.cancel(IllegalStateException("Starting new continuation while one already exists"))
            activityContinuation = cont
        }
    }

    @JvmOverloads
    fun <R> withActivityResult(intent: Intent, context: CoroutineContext= UI, body: (ActivityResult) -> R) {
        async(context, start = CoroutineStart.UNDISPATCHED) {
            body(activityResult(intent))
        }
    }


    @SuppressLint("RestrictedApi")
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }

    companion object {
        const val REQUEST_CODE_START = 0xf00;
    }

}

typealias ParcelableContinuation<T> = CancellableContinuation<T>

//interface ParcelableContinuation<T>: CancellableContinuation<T>

sealed class ActivityResult {
    object Cancelled: ActivityResult()
    data class Ok(val data: Intent?):ActivityResult()
}
