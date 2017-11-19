package nl.adaptivity.android.darwin

import android.app.Activity
import android.content.Context

/**
 * Created by pdvrieze on 19/11/17.
 */
interface AuthenticationContext {
    val context: Context get() = activity
    val activity: Activity
    val downloadRequestCode: Int
}