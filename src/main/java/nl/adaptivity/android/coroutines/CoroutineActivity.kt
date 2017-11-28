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
import kotlinx.coroutines.experimental.CancellableContinuation
import nl.adaptivity.android.kotlin.bundle
import nl.adaptivity.android.kotlin.set
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
    val contFragment = RetainedContinuationFragment(ParcelableContinuation(body, COROUTINEFRAGMENT_RESULTCODE_START))
    fragmentManager.beginTransaction().add(contFragment,RetainedContinuationFragment.TAG).commit()
    runOnUiThread {
        fragmentManager.executePendingTransactions()
        contFragment.startActivityForResult(intent, COROUTINEFRAGMENT_RESULTCODE_START)
    }
}

suspend fun Activity.activityResult(intent:Intent): ActivityResult {
    return suspendCoroutine { continuation ->
        val fm = fragmentManager
        val contFragment = RetainedContinuationFragment(ParcelableContinuation(continuation, COROUTINEFRAGMENT_RESULTCODE_START))

        fm.beginTransaction().apply {
            // This shouldn't happen, but in that case remove the old continuation.
            fm.findFragmentByTag(RetainedContinuationFragment.TAG)?.let { remove(it) }

            add(contFragment, RetainedContinuationFragment.TAG)

            commit()
        }


        runOnUiThread {
            fm.executePendingTransactions()
            contFragment.startActivityForResult(intent, COROUTINEFRAGMENT_RESULTCODE_START)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
fun <A: Activity> A.withActivityResult(intent: Intent, options: Bundle?, body: SerializableHandler<A, ActivityResult>) {
    // Horrible hack to fix generics
    @Suppress("UNCHECKED_CAST")
    val contFragment = RetainedContinuationFragment(ParcelableContinuation(body, COROUTINEFRAGMENT_RESULTCODE_START))
    fragmentManager.beginTransaction().add(contFragment,RetainedContinuationFragment.TAG).commit()
    runOnUiThread {
        fragmentManager.executePendingTransactions()
        contFragment.startActivityForResult(intent, COROUTINEFRAGMENT_RESULTCODE_START, options)
    }
}

const val COROUTINEFRAGMENT_RESULTCODE_START = 0xf00

const val KEY_ACTIVITY_CONTINUATION = "activityContinuation"

typealias SerializableHandler<A,T> = A.(T) -> Unit

fun <A:Activity, T> ParcelableContinuation(handler: SerializableHandler<A,T>, requestCode: Int = -1)
        = ParcelableContinuationCompat(handler, requestCode)

class ParcelableContinuationCompat<A:Activity, T> private constructor(requestCode: Int, handlerOrContinuation: Any): ParcelableContinuation<T>(requestCode, handlerOrContinuation) {

    constructor(handler: SerializableHandler<A, T>, requestCode: Int = -1): this(requestCode, handlerOrContinuation = handler)

    constructor(handler: Continuation<T>, requestCode: Int = -1): this(requestCode, handlerOrContinuation = handler)


    @Suppress("UNCHECKED_CAST")
    constructor(parcel: Parcel) :
            this(parcel.readInt(), handlerOrContinuation = ByteArray(parcel.readInt()).also { parcel.readByteArray(it) } ) {
        Log.d(TAG, "Read continuation from parcel")
    }

    private fun resolve(context:Context): Any {
        val h = continuation
        return when (h) {
            is ByteArray -> kryoAndroid(context).readClassAndObject(Input(h)).also { continuation = it }
            else -> h
        }
    }

    override fun resume(context: Context, value: T) {
        val h = resolve(context)
        when (h) {
            is Continuation<*> -> (h as Continuation<T>).resume(value)
            is Function<*> -> (h as SerializableHandler<Context, T>).invoke(context, value)
            else -> throw IllegalStateException("Invalid continuation: ${h::class.java.name}")
        }
    }

    override fun resumeWithException(context: Context, exception: Exception) {
        val h = resolve(context)
        when (h) {
            is Continuation<*> -> h.resumeWithException(exception)
            is Function<*> -> (h as SerializableHandler<Context, T?>).invoke(context, null)
            else -> throw IllegalStateException("Invalid continuation: ${h::class.java.name}")
        }
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<ParcelableContinuationCompat<Activity, Any?>> {
        override fun createFromParcel(parcel: Parcel): ParcelableContinuationCompat<Activity, Any?> {
            return ParcelableContinuationCompat(parcel)
        }

        override fun newArray(size: Int): Array<ParcelableContinuationCompat<Activity, Any?>?> {
            return arrayOfNulls(size)
        }

        @JvmStatic
        val TAG = ParcelableContinuationCompat::class.java.simpleName
    }
}

open class ParcelableContinuation<T> protected constructor(val requestCode: Int, protected var continuation: Any): Parcelable {

    constructor(handler: Continuation<T>, requestCode: Int = -1): this(requestCode, handler)


    @Suppress("UNCHECKED_CAST")
    constructor(parcel: Parcel) :
            this(parcel.readInt(), continuation = ByteArray(parcel.readInt()).also { parcel.readByteArray(it) } ) {
        Log.d(TAG, "Read continuation from parcel")
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        Log.d(TAG, "Writing continuation to parcel")
        dest.writeInt(requestCode)
        val h = continuation
        if (h is ByteArray) {
            try {
                dest.writeInt(h.size)
                dest.writeByteArray(h)
            } catch (e: Exception) {
                Log.e(TAG, "Error writing bytearray of previous continuation: ${e.message}", e)
                throw e
            }
        } else {
            try {
                dest.writeKryoObject(h, kryoAndroid)
            } catch (e: Exception) {
                Log.e(TAG, "Error writing continuation: ${e.message}", e)
                throw e
            }
        }
    }

    open fun cancel(context: Context) {
        val continuation = resolve(context)
        if (continuation is CancellableContinuation) {
            continuation.cancel()
        } else {
            (continuation as Continuation<T?>).resume(null)
        }
    }

    open fun resume(context: Context, value: T) {
        resolve(context).resume(value)
    }

    open fun resumeWithException(context: Context, exception: Exception) {
        resolve(context).resumeWithException(exception)
    }

    private fun resolve(context:Context): Continuation<T> {
        val h = continuation
        return when (h) {
            is ByteArray -> (kryoAndroid(context).readClassAndObject(Input(h)) as Continuation<T>).also { continuation = it }
            else -> h as Continuation<T>
        }
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<ParcelableContinuation<Any?>> {
        override fun createFromParcel(parcel: Parcel): ParcelableContinuation<Any?> {
            return ParcelableContinuation(parcel)
        }

        override fun newArray(size: Int): Array<ParcelableContinuation<Any?>?> {
            return arrayOfNulls(size)
        }

        @JvmStatic
        val TAG = ParcelableContinuation::class.java.simpleName
    }
}

typealias ActivityResult = Maybe<Intent?>

sealed class Maybe<T> {

    data class Error(val e: Exception): Maybe<Nothing>() {
        override fun <R> flatMap(function: (Nothing) -> R): Nothing {
            throw e
        }

        override fun <T> select(ok: T, cancelled: T, error: T) = error
    }

    object Cancelled: Maybe<Nothing>() {
        override fun <R> flatMap(function: (Nothing) -> R) = null
        override fun <T> select(ok: T, cancelled: T, error: T) = cancelled
    }

    data class Ok<T>(val data: T): Maybe<T>() {
        override fun <R> flatMap(function: (T) -> R): R = function(data)
        override fun <U> select(ok: U, cancelled: U, error: U) = ok
    }

    abstract fun <R> flatMap(function: (T) -> R): R?

    fun <R> map(function: (T) -> R): Maybe<R> {
        @Suppress("UNCHECKED_CAST")
        return when(this) {
            is Ok -> Ok(function(data))
            else -> this as Maybe<R>
        }
    }

    inline fun <R> onError(function: Error.(Exception) -> R):R? = if (this is Error) function(e) else null
    inline fun <R> onCancelled(function: Cancelled.() -> R):R? = if (this is Cancelled) function() else null
    inline fun <R> onOk(function: Ok<in T>.(T) -> R):R? = if (this is Ok) function(data) else null

    abstract fun <T> select(ok: T, cancelled:T, error: T):T

    companion object {
        inline fun <T> error(e: Exception): Maybe<T> = Error(e) as Maybe<T>

        inline fun <T> cancelled(): Maybe<T> = Cancelled as Maybe<T>
    }

}

private fun RetainedContinuationFragment(activityContinuation: ParcelableContinuation<Maybe<Intent?>>) = RetainedContinuationFragment().also {
    it.arguments = bundle(1) { it[KEY_ACTIVITY_CONTINUATION]= activityContinuation }
}

class RetainedContinuationFragment : Fragment() {
    private var activityContinuation: ParcelableContinuation<ActivityResult>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        arguments?.getParcelable<ParcelableContinuation<ActivityResult>>(KEY_ACTIVITY_CONTINUATION)?.let { activityContinuation = it }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("UNCHECKED_CAST")
        fun dispatchResult(cont: ParcelableContinuation<ActivityResult>, activityResult: ActivityResult) {
            cont.resume(activity, activityResult)
            activityContinuation = null
        }

        val cont = activityContinuation
        when {
            cont == null  ||
                    requestCode != cont.requestCode -> super.onActivityResult(requestCode, resultCode, data)
            resultCode == Activity.RESULT_OK -> dispatchResult(cont, Maybe.Ok(data))
            resultCode == Activity.RESULT_CANCELED -> dispatchResult(cont, Maybe.cancelled())
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
        // Remove this fragment, it's no longer needed
        fragmentManager.beginTransaction().remove(this).commit()

    }

    companion object {
        const val TAG = "__RETAINED_CONTINUATION_FRAGMENT__"
    }
}