package nl.adaptivity.android.darwin

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.*
import android.support.annotation.RequiresApi
import android.support.v4.app.SupportActivity
import java.io.Serializable

/**
 * Activity class that has additional support for coroutines
 */
open class CoroutineActivity: SupportActivity() {
    private var activityContinuation: ParcelableContinuation<CoroutineActivity, ActivityResult>? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val cont = activityContinuation
        when {
            cont == null  ||
            requestCode != cont.requestCode -> super.onActivityResult(requestCode, resultCode, data)
            resultCode == Activity.RESULT_OK -> {
                activityContinuation = null
                cont.handler(this, ActivityResult.Ok(data))
            }
            resultCode == Activity.RESULT_CANCELED -> {
                activityContinuation = null
                cont.handler(this, ActivityResult.Cancelled)
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    fun <A:CoroutineActivity> withActivityResult(intent: Intent, body: SerializableHandler<A, ActivityResult>) {
        startActivityForResult(intent, REQUEST_CODE_START)
        // Horrible hack to fix generics
        activityContinuation = ParcelableContinuation(REQUEST_CODE_START, body) as ParcelableContinuation<CoroutineActivity, ActivityResult>
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    fun <A:CoroutineActivity> withActivityResult(intent: Intent, options: Bundle?, body: SerializableHandler<A, ActivityResult>) {
        startActivityForResult(intent, REQUEST_CODE_START, options)
        activityContinuation = ParcelableContinuation(REQUEST_CODE_START, body) as ParcelableContinuation<CoroutineActivity, ActivityResult>
    }


    @SuppressLint("RestrictedApi")
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        activityContinuation?.let {
            outState.putParcelable(KEY_ACTIVITY_CONTINUATION, activityContinuation)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        activityContinuation = savedInstanceState.getParcelable<ParcelableContinuation<CoroutineActivity, ActivityResult>>(KEY_ACTIVITY_CONTINUATION)
    }

    companion object {
        const val REQUEST_CODE_START = 0xf00;
        const val KEY_ACTIVITY_CONTINUATION = "activityContinuation"
    }

    interface SerializableHandler<A: Activity, T>: Serializable {
        operator fun invoke(activity: A, result: T)
    }

    private class ParcelableContinuation<A:Activity, T>(val requestCode: Int, val handler: SerializableHandler<A,T>): Parcelable {
        @Suppress("UNCHECKED_CAST")
        constructor(parcel: Parcel) :
                this(parcel.readInt(), parcel.readSerializable() as SerializableHandler<A, T>) {
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeInt(requestCode)
            dest.writeSerializable(handler)
        }

        override fun describeContents() = 0

        companion object CREATOR : Parcelable.Creator<ParcelableContinuation<Activity, Any?>> {
            override fun createFromParcel(parcel: Parcel): ParcelableContinuation<Activity, Any?> {
                return ParcelableContinuation(parcel)
            }

            override fun newArray(size: Int): Array<ParcelableContinuation<Activity, Any?>?> {
                return arrayOfNulls(size)
            }
        }
    }

}

sealed class ActivityResult {
    object Cancelled: ActivityResult()
    data class Ok(val data: Intent?):ActivityResult()
}
