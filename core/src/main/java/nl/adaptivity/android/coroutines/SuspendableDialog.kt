package nl.adaptivity.android.coroutines

import android.app.Activity
import android.app.DialogFragment
import android.content.DialogInterface
import android.os.Bundle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Base class for dialog fragments that support coroutine based dialog invocation. Direct instantiation
 * probably makes no sense, subclassing is expected.
 */
open class SuspendableDialog<T>: DialogFragment() {


    private var callback: ParcelableContinuation<Maybe<T>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.getParcelable<ParcelableContinuation<Maybe<T>>>("continutation").let { callback = it }
    }

    /**
     * Actually show the fragment and get the result. This requires the dialog
     * code to invoke [dispatchResult] on succesful completion.
     */
    suspend fun show(activity: Activity, tag: String) : Maybe<T> {
        super.show(activity.fragmentManager, tag)
        val d = this
        return suspendCancellableCoroutine { cont ->
            callback?.cancel(activity)
            callback = ParcelableContinuation(cont, activity)
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
            callback.cancel(activity, CancellationException("Dialog dismissed"))
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
            callback.cancel(activity, CancellationException("Dialog dismissed"))
        }
    }

    /**
     * Subclasses must call this to resume [show] with the expected result.
     */
    protected fun dispatchResult(resultValue: T) {
        callback?.let { callback ->
            this.callback = null // Set the property to null to prevent reinvocation
            callback.resume(activity, Maybe.Ok(resultValue))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("continutation", callback)
    }
}

@Deprecated("Compatibility alias, as Maybe should be used, not DialogResult", ReplaceWith("Maybe<T>"))
typealias DialogResult<T> = Maybe<T>