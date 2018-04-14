package nl.adaptivity.android.coroutines

import android.app.Activity
import android.app.Fragment
import android.os.Bundle
import kotlin.coroutines.experimental.Continuation

/**
 * Base class for fragments that are used to store continuations.
 */
open class BaseRetainedContinuationFragment<T> : Fragment() {
    private val parcelableContinuations = arrayListOf<ParcelableContinuation<*>>()

    @Deprecated("This is quite unsafe")
    protected val requestCode: Int get() = parcelableContinuations.firstOrNull()?.requestCode ?: -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true

        if (savedInstanceState!=null) {
            savedInstanceState.getParcelableArrayList<ParcelableContinuation<T>>(KEY_ACTIVITY_CONTINUATIONS)?.let {
                parcelableContinuations.addAll(it)
            }
        } else {
            arguments?.getParcelableArrayList<ParcelableContinuation<T>>(KEY_ACTIVITY_CONTINUATIONS)?.let {
                parcelableContinuations.apply {
                    addAll(it)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList(KEY_ACTIVITY_CONTINUATIONS, parcelableContinuations)
    }

    fun addContinuation(parcelableContinuation: ParcelableContinuation<ActivityResult>) {
        parcelableContinuations.add(parcelableContinuation)
    }

    override fun onAttach(activity: Activity?) {
        super.onAttach(activity)
        for (parcelableContinuation in parcelableContinuations) {
            parcelableContinuation.attachContext(activity)
        }
    }

    override fun onDetach() {
        super.onDetach()
        for (parcelableContinuation in parcelableContinuations) {
            parcelableContinuation.attachContext(activity)
        }
    }

    protected fun dispatchResult(activityResult: T, requestCode: Int) {
        @Suppress("UNCHECKED_CAST")
        val continuation = parcelableContinuations.single { it.requestCode == requestCode } as ParcelableContinuation<T>
        continuation.resume(activity, activityResult)
        parcelableContinuations.remove(continuation)

        if (parcelableContinuations.isEmpty()) {
            // Remove this fragment, it's no longer needed
            fragmentManager.beginTransaction().remove(this).commit()
        }

    }

    fun setContinuation(continuation: Continuation<T>) {
        setContinuation(ParcelableContinuation<T>(continuation, attachedContext = activity))
    }

    @Deprecated("Allow multiple continuations", ReplaceWith("addContinuation(continuation)"))
    fun setContinuation(continuation: ParcelableContinuation<T>) {
        (arguments ?: Bundle(1).also { arguments=it }).putParcelableArrayList(KEY_ACTIVITY_CONTINUATIONS, arrayListOf(continuation))
    }
}