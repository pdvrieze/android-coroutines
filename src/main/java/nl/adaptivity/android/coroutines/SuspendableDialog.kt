package nl.adaptivity.android.coroutines

import android.app.Activity
import android.content.DialogInterface
import android.support.v4.app.DialogFragment
import kotlinx.coroutines.experimental.CancellableContinuation
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.suspendCancellableCoroutine


open class SuspendableDialog<T>: DialogFragment() {


    private var callback: CancellableContinuation<DialogResult<T>>? = null

    suspend fun show(activity: Activity, requestCode: Int) : DialogResult<T> {
        val d = this
        return suspendCancellableCoroutine<DialogResult<T>> { cont ->
            callback?.cancel()
            callback = cont
        }
    }

    override fun onDismiss(dialog: DialogInterface?) {
        super.onDismiss(dialog)
        callback?.let { callback ->
            this.callback = null // Set the property to null to prevent reinvocation
            callback.cancel(CancellationException("Dialog dismissed"))
        }
    }

    override fun onCancel(dialog: DialogInterface?) {
        super.onCancel(dialog)
        callback?.let { callback ->
            this.callback = null // Set the property to null to prevent reinvocation
            callback.cancel(CancellationException("Dialog dismissed"))
        }
    }

    protected fun dispatchResult(resultValue: T) {
        callback?.let { callback ->
            this.callback = null // Set the property to null to prevent reinvocation
            callback.resume(DialogResult.Success(resultValue))
        }
    }
}

/**
 * Class representing the result of a dialog
 */
sealed class DialogResult<T> {
    private object Cancelled: DialogResult<Any>()

    @Suppress("UNCHECKED_CAST")
    fun <T> Cancelled(): DialogResult<T> = Cancelled as DialogResult<T>

    data class Success<T>(val value: T): DialogResult<T>()
}