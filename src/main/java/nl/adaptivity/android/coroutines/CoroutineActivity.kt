package nl.adaptivity.android.coroutines

import android.app.Activity
import android.app.Fragment
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.support.annotation.RequiresApi
import android.util.Log
import com.esotericsoftware.kryo.io.Input
import nl.adaptivity.android.kotlin.bundle
import nl.adaptivity.android.kotlin.getValue
import nl.adaptivity.android.kotlin.set
import nl.adaptivity.android.kotlin.weakRef
import nl.adaptivity.android.kryo.kryoAndroid
import nl.adaptivity.android.kryo.writeKryoObject
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * Function that starts an activity and uses a callback on the result.
 */
fun <A:Activity> A.withActivityResult(intent: Intent, body: SerializableHandler<A, ActivityResult>) {
    // Horrible hack to fix generics
    @Suppress("UNCHECKED_CAST")
    val contFragment = RetainedContinuationFragment(ParcelableContinuation(COROUTINEFRAGMENT_RESULTCODE_START, body) as ParcelableContinuation<Activity, ActivityResult>)
    fragmentManager.beginTransaction().add(contFragment,RetainedContinuationFragment.TAG).commit()
    runOnUiThread {
        fragmentManager.executePendingTransactions()
        contFragment.startActivityForResult(intent, COROUTINEFRAGMENT_RESULTCODE_START)
    }
}

suspend fun Activity.activityResult(intent:Intent):ActivityResult {
    return suspendCoroutine { continuation ->
        val contFragment = RetainedContinuationFragment(ParcelableContinuation<Activity, ActivityResult>(COROUTINEFRAGMENT_RESULTCODE_START, continuation))
        fragmentManager.beginTransaction().add(contFragment, RetainedContinuationFragment.TAG).commit()
        runOnUiThread {
            fragmentManager.executePendingTransactions()
            contFragment.startActivityForResult(intent, COROUTINEFRAGMENT_RESULTCODE_START)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
fun <A: Activity> A.withActivityResult(intent: Intent, options: Bundle?, body: SerializableHandler<A, ActivityResult>) {
    // Horrible hack to fix generics
    @Suppress("UNCHECKED_CAST")
    val contFragment = RetainedContinuationFragment(ParcelableContinuation(COROUTINEFRAGMENT_RESULTCODE_START, body) as ParcelableContinuation<Activity, ActivityResult>)
    fragmentManager.beginTransaction().add(contFragment,RetainedContinuationFragment.TAG).commit()
    runOnUiThread {
        fragmentManager.executePendingTransactions()
        contFragment.startActivityForResult(intent, COROUTINEFRAGMENT_RESULTCODE_START, options)
    }
}

const val COROUTINEFRAGMENT_RESULTCODE_START = 0xf00

const val KEY_ACTIVITY_CONTINUATION = "activityContinuation"

typealias SerializableHandler<A,T> = A.(T) -> Unit

private class ParcelableContinuation<A:Activity, T> private constructor(val requestCode: Int, var handlerOrContinuation: Any): Parcelable {

    constructor(requestCode: Int, handler: SerializableHandler<A, T>): this(requestCode, handlerOrContinuation = handler)

    constructor(requestCode: Int, handler: Continuation<T>): this(requestCode, handlerOrContinuation = handler)


    @Suppress("UNCHECKED_CAST")
    constructor(parcel: Parcel) :
            this(parcel.readInt(), handlerOrContinuation = ByteArray(parcel.readInt()).also { parcel.readByteArray(it) } ) {
        Log.d(TAG, "Read continuation from parcel")
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        Log.d(TAG, "Writing continuation to parcel")
        dest.writeInt(requestCode)
        val h = handlerOrContinuation
        if (h is ByteArray) {
            dest.unmarshall(h, 0, h.size)
        } else {
            dest.writeKryoObject(h, kryoAndroid)
        }
    }

    fun resolve(context:Context): Any {
        val h = handlerOrContinuation
        return when (h) {
            is ByteArray -> kryoAndroid(context).readClassAndObject(Input(h))
            else -> h
        }
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
        val TAG = ParcelableContinuation::class.java.simpleName
    }
}

sealed class ActivityResult {
    object Cancelled: ActivityResult() {
        override fun <R> map(function: (Intent?) -> R) = null
    }

    data class Ok(val data: Intent?): ActivityResult() {
        override fun <R> map(function: (Intent?) -> R): R = function(data)
    }

    abstract fun <R> map(function: (Intent?) -> R): R?
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
        @Suppress("UNCHECKED_CAST")
        fun dispatchResult(cont: ParcelableContinuation<Activity, ActivityResult>, activityResult: ActivityResult) {
            val handler = cont.resolve(activity)
            activityContinuation = null
            when (handler) {
                is Continuation<*> -> (handler as Continuation<ActivityResult>).resume(activityResult)
                is Function2<*,*,*> -> (handler as Activity.(ActivityResult) -> Unit).invoke(activity, activityResult)
                else -> throw IllegalStateException("Should be unreachable, handler is: ${handler.javaClass.canonicalName}")
            }

        }

        val cont = activityContinuation
        when {
            cont == null  ||
                    requestCode != cont.requestCode -> super.onActivityResult(requestCode, resultCode, data)
            resultCode == Activity.RESULT_OK -> dispatchResult(cont, ActivityResult.Ok(data))
            resultCode == Activity.RESULT_CANCELED -> dispatchResult(cont, ActivityResult.Cancelled)
            else -> super.onActivityResult(requestCode, resultCode, data)
        }

    }

    companion object {
        const val TAG = "__RETAINED_CONTINUATION_FRAGMENT__"
    }
}