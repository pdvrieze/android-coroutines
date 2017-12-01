package nl.adaptivity.android.coroutines

import android.app.Activity
import android.content.Intent

class RetainedContinuationFragment : BaseRetainedContinuationFragment<Maybe<Intent?>>() {

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when {
            requestCode != this.requestCode -> super.onActivityResult(requestCode, resultCode, data)
            resultCode == Activity.RESULT_OK -> dispatchResult(Maybe.Ok(data))
            resultCode == Activity.RESULT_CANCELED -> dispatchResult(Maybe.cancelled())
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    companion object {
        const val TAG = "__RETAINED_CONTINUATION_FRAGMENT__"
    }
}

@Suppress("FunctionName")
fun RetainedContinuationFragment(activityContinuation: ParcelableContinuation<Maybe<Intent?>>) = RetainedContinuationFragment().also {
    it.setContinuation(activityContinuation)
}