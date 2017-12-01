package nl.adaptivity.android.util

import android.content.pm.PackageManager
import java.util.*

/**
 * Class representing the result of a permission request
 * @property permissions The permissions requested
 * @property grantResults The result of the request
 */
data class GrantResult(val permissions: Array<out String>, val grantResults: IntArray) {
    /**
     * Convenience method to check whether a permission was granted.
     */
    fun wasGranted(permission:String): Boolean {
        val index = permissions.indexOf(permission).also { if (it<0) return false }
        return grantResults[index] == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Convenience property to determine whether all permissions requested were granted.
     */
    val allGranted: Boolean = grantResults.all { it== PackageManager.PERMISSION_GRANTED }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GrantResult

        if (!Arrays.equals(permissions, other.permissions)) return false
        if (!Arrays.equals(grantResults, other.grantResults)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = Arrays.hashCode(permissions)
        result = 31 * result + Arrays.hashCode(grantResults)
        return result
    }
}