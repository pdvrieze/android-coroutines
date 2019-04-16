package nl.adaptivity.android.coroutines

import android.app.Fragment
import android.os.Bundle
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

/**
 * Base class for fragments that are used to store continuations.
 */
open class BaseRetainedContinuationFragment<T> : Fragment() {
    private val parcelableContinuations = arrayListOf<ParcelableContinuation<*>>()

    @Deprecated("This is quite unsafe")
    protected val requestCode: Int get() = parcelableContinuations.firstOrNull()?.requestCode ?: -1
    val lastResultCode: Int get() {
        return parcelableContinuations.maxBy { it.requestCode }?.requestCode ?: (COROUTINEFRAGMENT_RESULTCODE_START-1)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true

        if (savedInstanceState!=null) {
            savedInstanceState.getParcelableArrayList<ParcelableContinuation<T>>(KEY_ACTIVITY_CONTINUATIONS_STATE)?.let {
                parcelableContinuations.addAll(it)
            }
        }
        for (parcelableContinuation in parcelableContinuations) {
            parcelableContinuation.attachContext2(activity)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Make sure to store the state now rather than later so that we actually know the fragment id and tags etc.
        parcelableContinuations.forEach{ it.detachContext() }

        outState.putParcelableArrayList(KEY_ACTIVITY_CONTINUATIONS_STATE, parcelableContinuations)
    }

    fun addContinuation(parcelableContinuation: ParcelableContinuation<T>) {
        parcelableContinuations.add(parcelableContinuation)
    }

    protected fun dispatchResult(activityResult: T, requestCode: Int) {
        fragmentManager.executePendingTransactions()
        @Suppress("UNCHECKED_CAST")
        val continuation = parcelableContinuations.single { it.requestCode == requestCode } as ParcelableContinuation<T>
        continuation.resume(activity, activityResult)
        parcelableContinuations.remove(continuation)

        if (parcelableContinuations.isEmpty()) {
            // Remove this fragment, it's no longer needed
            fragmentManager.beginTransaction().remove(this).commit()
        }

    }

}