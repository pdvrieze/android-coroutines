package nl.adaptivity.android.coroutines

import android.app.Activity
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import com.esotericsoftware.kryo.io.Input
import nl.adaptivity.android.kryo.kryoAndroid
import kotlin.coroutines.experimental.Continuation

/**
 * Java compatibility helper factory method
 */
@Suppress("FunctionName")
@JvmOverloads
fun <A: Activity, T> ParcelableContinuation(handler: SerializableHandler<A, T>, requestCode: Int = -1)
        = ParcelableContinuationCompat<A, T>({ handler(this, it) }, requestCode)

@Suppress("FunctionName")
fun <A: Activity, T> ParcelableContinuation(handler: A.(T) -> Unit, requestCode: Int = -1)
        = ParcelableContinuationCompat<A, T>({ handler(this, it) }, requestCode)

/**
 * [ParcelableContinuation] subclass that not only works with continuations, but also handles
 * Java and Kotlin callback lambdas.
 *
 * @param requestCode The request code that this continuation should resume on.
 * @param handlerOrContinuation The executable object that handles the result.
 */
class ParcelableContinuationCompat<A: Activity, T> private constructor(requestCode: Int, handlerOrContinuation: Any): ParcelableContinuation<T>(requestCode, handlerOrContinuation) {

    /**
     * Create a new  continuation with a lambda function callback.
     */
    constructor(handler: A.(T) -> Unit, requestCode: Int = -1): this(requestCode, handlerOrContinuation = handler)

    /**
     * Create a new continuation with a continuation callback.
     */
    @Suppress("unused")
    constructor(handler: Continuation<T>, requestCode: Int = -1): this(requestCode, handlerOrContinuation = handler)


    /**
     * Inflate the object from the parcel.
     * @param parcel The parcel to inflate from
     *
     * @see Parcelable.Creator.createFromParcel
     */
    @Suppress("UNCHECKED_CAST")
    constructor(parcel: Parcel) :
            this(parcel.readInt(), handlerOrContinuation = ByteArray(parcel.readInt()).also { parcel.readByteArray(it) } ) {
        Log.d(TAG, "Read continuation from parcel")
    }

    /**
     * Helper function that performs the delayed deflation (from the byte array Kryo creates).
     */
    private fun resolve(context: Context): Any {
        val h = continuation
        return when (h) {
            is ByteArray -> kryoAndroid(context).readClassAndObject(Input(h)).also { continuation = it }
            else -> h
        }
    }

    override fun resume(context: Context, value: T) {
        val h = resolve(context)
        @Suppress("UNCHECKED_CAST")
        when (h) {
            is Continuation<*> -> (h as Continuation<T>).resume(value)
            is Function<*> -> (h as Context.(T?)->Unit).invoke(context, value)
            else -> throw IllegalStateException("Invalid continuation: ${h::class.java.name}")
        }
    }

    override fun resumeWithException(context: Context, exception: Throwable) {
        val h = resolve(context)
        @Suppress("UNCHECKED_CAST")
        when (h) {
            is Continuation<*> -> h.resumeWithException(exception)
            is Function<*> -> (h as Context.(T?)->Unit).invoke(context, null)
            else -> throw IllegalStateException("Invalid continuation: ${h::class.java.name}")
        }
    }

    /**
     * Helper class for [Parcelable]
     * @see Parcelable.Creator
     */
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