package nl.adaptivity.android.darwin

import android.app.Activity
import android.content.Context

/**
 * Helper interface. Not sure that it is needed.
 */
interface AuthenticationContext {
    val context: Context get() = activity
    val activity: Activity
    val downloadRequestCode: Int
}