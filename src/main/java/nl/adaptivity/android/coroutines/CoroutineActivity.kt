package nl.adaptivity.android.coroutines

import android.app.Activity
import android.app.Fragment
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.support.annotation.RequiresApi
import android.util.Log
import nl.adaptivity.android.kotlin.bundle
import nl.adaptivity.android.kotlin.set
import java.io.Serializable

/**
 * Function that starts an activity and uses a callback on the result.
 */
fun <A:Activity> A.withActivityResult(intent: Intent, body: SerializableHandler<A, ActivityResult>) {
    // Horrible hack to fix generics
    @Suppress("UNCHECKED_CAST")
    val contFragment = RetainedContinuationFragment(ParcelableContinuation(COROUTINEFRAGMENT_RESULTCODE_START, body) as ParcelableContinuation<Activity, ActivityResult>)
    fragmentManager.beginTransaction().add(contFragment,RetainedContinuationFragment.TAG).commit()
    fragmentManager.executePendingTransactions()
    contFragment.startActivityForResult(intent, COROUTINEFRAGMENT_RESULTCODE_START)
}

@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
fun <A: Activity> A.withActivityResult(intent: Intent, options: Bundle?, body: SerializableHandler<A, ActivityResult>) {
    // Horrible hack to fix generics
    @Suppress("UNCHECKED_CAST")
    val contFragment = RetainedContinuationFragment(ParcelableContinuation(COROUTINEFRAGMENT_RESULTCODE_START, body) as ParcelableContinuation<Activity, ActivityResult>)
    fragmentManager.beginTransaction().add(contFragment,RetainedContinuationFragment.TAG).commit()
    fragmentManager.executePendingTransactions()
    contFragment.startActivityForResult(intent, COROUTINEFRAGMENT_RESULTCODE_START, options)
}

const val COROUTINEFRAGMENT_RESULTCODE_START = 0xf00;

const val KEY_ACTIVITY_CONTINUATION = "activityContinuation"

typealias SerializableHandler<A,T> = A.(T) -> Unit

private class ParcelableContinuation<A:Activity, T>(val requestCode: Int, val handler: SerializableHandler<A, T>): Parcelable {
    @Suppress("UNCHECKED_CAST")
    constructor(parcel: Parcel) :
            this(parcel.readInt(), parcel.readSerializable() as SerializableHandler<A, T>) {
        Log.d(TAG, "Read continuation from parcel")
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        Log.d(TAG, "Writing continuation to parcel")
        dest.writeInt(requestCode)
        dest.writeSerializable(handler as Serializable)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<ParcelableContinuation<Activity, Any?>> {
        override fun createFromParcel(parcel: Parcel): ParcelableContinuation<Activity, Any?> {
            return ParcelableContinuation(parcel)
        }

        override fun newArray(size: Int): Array<ParcelableContinuation<Activity, Any?>?> {
            return arrayOfNulls(size)
        }

        @JvmStatic
        val TAG = ParcelableContinuation.javaClass.simpleName
    }
}

sealed class ActivityResult {
    object Cancelled: ActivityResult()
    data class Ok(val data: Intent?): ActivityResult()
}

private fun RetainedContinuationFragment(activityContinuation: ParcelableContinuation<Activity, ActivityResult>) = RetainedContinuationFragment().also {
    it.arguments = bundle(1) { it[KEY_ACTIVITY_CONTINUATION]= activityContinuation }
}

class RetainedContinuationFragment : Fragment() {
    private var activityContinuation: ParcelableContinuation<Activity, ActivityResult>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        arguments?.getParcelable<ParcelableContinuation<Activity, ActivityResult>>(KEY_ACTIVITY_CONTINUATION)?.let { activityContinuation = it }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val cont = activityContinuation
        when {
            cont == null  ||
                    requestCode != cont.requestCode -> super.onActivityResult(requestCode, resultCode, data)
            resultCode == Activity.RESULT_OK -> {
                activityContinuation = null
                cont.handler(activity, ActivityResult.Ok(data))
            }
            resultCode == Activity.RESULT_CANCELED -> {
                activityContinuation = null
                cont.handler(activity, ActivityResult.Cancelled)
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    companion object {
        const val TAG = "__RETAINED_CONTINUATION_FRAGMENT__"
    }
}