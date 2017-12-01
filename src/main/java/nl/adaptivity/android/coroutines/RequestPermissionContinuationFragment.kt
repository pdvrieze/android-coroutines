package nl.adaptivity.android.coroutines

import android.content.Intent
import android.content.pm.PackageManager
import nl.adaptivity.android.util.GrantResult

class RequestPermissionContinuationFragment : BaseRetainedContinuationFragment<GrantResult?>() {

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when {
            requestCode != this.requestCode -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            grantResults.isEmpty() || grantResults.all { it==PackageManager.PERMISSION_DENIED } -> dispatchResult(null)
            else -> dispatchResult(GrantResult(permissions, grantResults))
        }
    }

    companion object {
        const val TAG = "__REQUEST_PERMISSION_CONTINUATION_FRAGMENT__"
    }
}

@Suppress("FunctionName")
fun RequestPermissionContinuationFragment(activityContinuation: ParcelableContinuation<GrantResult?>) = RequestPermissionContinuationFragment().also {
    it.setContinuation(activityContinuation)
}