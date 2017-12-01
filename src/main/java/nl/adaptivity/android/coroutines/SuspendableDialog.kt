package nl.adaptivity.android.coroutines

import android.app.Activity
import android.app.DialogFragment
import android.content.DialogInterface
import kotlinx.coroutines.experimental.CancellableContinuation
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.suspendCancellableCoroutine

/**
 * Base class for dialog fragments that support coroutine based dialog invocation. Direct instantiation
 * probably makes no sense, subclassing is expected.
 */
class SuspendableDialog<T>: DialogFragment() {


    private var callback: CancellableContinuation<DialogResult<T>>? = null

    /**
     * Actually show the fragment and get the result. This requires the dialog
     * code to invoke [dispatchResult] on succesful completion.
     */
    suspend fun show(activity: Activity, tag: String) : DialogResult<T> {
        super.show(activity.fragmentManager, tag)
        val d = this
        return suspendCancellableCoroutine<DialogResult<T>> { cont ->
            callback?.cancel()
            callback = cont
        }
    }

    /**
     * Not only implement the standard functionality, but also use this as a cancellation on
     * the dialog. If the continuation was not cancellable this will equal to resuming with a
     * null result.
     */
    override fun onDismiss(dialog: DialogInterface?) {
        super.onDismiss(dialog)
        callback?.let { callback ->
            this.callback = null // Set the property to null to prevent reinvocation
            callback.cancel(CancellationException("Dialog dismissed"))
        }
    }

    /**
     * Not only implement the standard functionality, but also use this as a cancellation on
     * the dialog. If the continuation was not cancellable this will equal to resuming with a
     * null result. Functionally equivalent to [onDismiss]
     */
    override fun onCancel(dialog: DialogInterface?) {
        super.onCancel(dialog)
        callback?.let { callback ->
            this.callback = null // Set the property to null to prevent reinvocation
            callback.cancel(CancellationException("Dialog dismissed"))
        }
    }

    /**
     * Subclasses must call this to resume [show] with the expected result.
     */
    protected fun dispatchResult(resultValue: T) {
        callback?.let { callback ->
            this.callback = null // Set the property to null to prevent reinvocation
            callback.resume(Maybe.Ok(resultValue))
        }
    }
}

@Deprecated("Compatibility alias, as Maybe should be used, not DialogResult", ReplaceWith("Maybe<T>"))
typealias DialogResult<T> = Maybe<T>