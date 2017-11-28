package nl.adaptivity.android.darwin

import android.app.Activity

enum class DarwinLibStatusEvents {
    /** The authenticator was succesfully downloaded */
    AUTHENTICATOR_DOWNLOAD_SUCCESS,
    /** The authenticator download failed */
    AUTHENTICATOR_DOWNLOAD_FAILURE,
    /** The user rejected downloading the authenticator */
    AUTHENTICATOR_DOWNLOAD_REJECTED,
    /** The authenticator download was cancelled */
    AUTHENTICATOR_DOWNLOAD_CANCELLED,
    AUTHENTICATOR_INSTALL_SUCCESS,
    AUTHENTICATOR_INSTALL_ERROR,
    AUTHENTICATOR_INSTALL_CANCELLED,
    ACCOUNT_SELECTED,
    ACCOUNT_SELECTION_CANCELLED,
    ACCOUNT_SELECTION_FAILED

}

/**
 * Interface for system that accepts status reports
 */
interface StatusReporter {
    fun reportStatus(event: DarwinLibStatusEvents, vararg extra: Any?)
}

fun Activity.reportStatus(event: DarwinLibStatusEvents, vararg extra:Any?) {
    (this as? StatusReporter)?.reportStatus(event, *extra)
}