package nl.adaptivity.android.coroutines

import android.app.Fragment
import android.os.Bundle
import kotlin.coroutines.experimental.Continuation

/**
 * Base class for fragments that are used to store continuations.
 */
open class BaseRetainedContinuationFragment<T> : Fragment() {
    private var parcelableContinuation: ParcelableContinuation<T>? = null

    protected val requestCode: Int = parcelableContinuation?.requestCode ?: -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        arguments?.getParcelable<ParcelableContinuation<T>>(KEY_ACTIVITY_CONTINUATION)?.let { parcelableContinuation = it }
    }

//    @Suppress("UNCHECKED_CAST")
    protected fun dispatchResult(activityResult: T) {
        val cont = parcelableContinuation
        cont?.resume(activity, activityResult)
        parcelableContinuation = null

        // Remove this fragment, it's no longer needed
        fragmentManager.beginTransaction().remove(this).commit()
    }

    fun setContinuation(continuation: Continuation<T>) {
        setContinuation(ParcelableContinuation<T>(continuation))
    }

    fun setContinuation(continuation: ParcelableContinuation<T>) {
        (arguments ?: Bundle(1).also { arguments=it }).putParcelable(KEY_ACTIVITY_CONTINUATION, continuation)
    }
}