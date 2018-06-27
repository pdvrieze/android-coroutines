package nl.adaptivity.android.coroutines

import android.app.Activity
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import kotlinx.coroutines.experimental.CancellableContinuation
import nl.adaptivity.android.kryo.kryoAndroid
import nl.adaptivity.android.kryo.writeKryoObject
import java.io.ByteArrayOutputStream
import kotlin.coroutines.experimental.Continuation

/**
 * This class is part of the magic of serializing continuations in Android. This class only works
 * with continuations, but [ParcelableContinuationCompat] extends it to work with callback lambda's
 * as well (for regular async code).
 *
 * While the code will serialize from the actual continuation, the deserialization will happen
 * in stages. This is required to support capture of android [Context] values in a sensible way
 * (actually serializing them is invalid).
 *
 * @property requestCode When started with [Activity.startActivityForResult] this is the request code that may be
 *                       used to match the continuation with it's start point. Currently ignored.
 * @property continuation The actual continuation that is stored/wrapped.
 */
open class ParcelableContinuation<T> protected constructor(val requestCode: Int, protected var continuation: Any, private var attachedContext: Context? = null): Parcelable {

    /**
     * Create a new instance for the given handler.
     */
    constructor(handler: Continuation<T>, attachedContext: Context?, requestCode: Int = -1): this(requestCode, handler, attachedContext)


    /**
     * Read the continuation from the parcel. This will merely store the continuation data as a byte array for
     * Kryo to deserialize later. (Note that the parcel cannot be validly stored).
     */
    @Suppress("UNCHECKED_CAST")
    constructor(parcel: Parcel) :
            this(parcel.readInt(), continuation = ByteArray(parcel.readInt()).also {
                parcel.readByteArray(it) } ) {
        Log.d(TAG, "Read continuation from parcel")
    }

    /**
     * Write the continuation (and requestCode) to a parcel for safe storage. This will handle the
     * case that the actual kryo data was still not deserialized and merely write it back to the new
     * parcel.
     */
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
                dest.writeKryoObject(h, kryoAndroid(attachedContext))
            } catch (e: Exception) {
                Log.e(TAG, "Error writing continuation: ${e.message}", e)
                throw e
            }
        }
    }

    /**
     * Cancel the continuation. This wraps [CancellableContinuation.cancel] but also reflates (when
     * needed) the continuation given the context. If the continuation is not a [CancellableContinuation]
     * this code will invoke [Continuation.resume]`(null)`.
     *
     * @param context The context to use for reflation. If not parcelled this is ignored.
     * @param cause The cause of the cancellation.
     * @see CancellableContinuation.cancel
     */
    @JvmOverloads
    open fun cancel(context: Context, cause: Throwable?=null) {
        val continuation = resolve(context)
        if (continuation is CancellableContinuation) {
            continuation.cancel(cause)
        } else {
            @Suppress("UNCHECKED_CAST")
            (continuation as Continuation<T?>).resume(null)
        }
    }

    /**
     * Resume the continuation while also using the context to reflate if needed.
     *
     * @param context The context to use for reflation. If not parcelled this is ignored.
     * @param value The result value to use for resumption.
     * @see Continuation.resume
     *
     */
    open fun resume(context: Context, value: T) {
        resolve(context).resume(value)
    }

    /**
     * Resume the continuation with an exception while also using the context to reflate if needed.

     * @param context The context to use for reflation. If not parcelled this is ignored.
     * @param exception The cause of the failure.
     * @see Continuation.resume
     */
    open fun resumeWithException(context: Context, exception: Throwable) {
        resolve(context).resumeWithException(exception)
    }

    /**
     * Helper function that does the deserialization.
     */
    private fun resolve(context: Context): Continuation<T> {
        if (attachedContext!=context) attachContext2(context)

        val h = continuation

        val continuation = when (h) {
            is ByteArray -> (kryoAndroid(context).readClassAndObject(Input(h)) as Continuation<T>).also { continuation = it }
            else -> h as Continuation<T>
        }

        when (context) {
            is Activity -> (continuation.context[ActivityContext] as ActivityContext<Activity>?)?.run { activity = context }
        }


        @Suppress("UNCHECKED_CAST")
        return continuation
    }

    override fun describeContents() = 0

    fun attachContext2(context: Context?) {
        val attachedContext = this.attachedContext
        when(attachedContext) {
            null -> this.attachedContext = context

            context -> Unit // do nothing

            else -> {
                if (continuation is Continuation<*>) {
                    val baos = ByteArrayOutputStream()
                    Output(baos).use { out -> kryoAndroid(attachedContext).writeClassAndObject(out, continuation) }

                    continuation = baos.toByteArray()
                }
                this.attachedContext = null
            }
        }
    }

    fun detachContext() {
        val attachedContext = this.attachedContext
        if (continuation is Continuation<*>) {
            val baos = ByteArrayOutputStream()
            Output(baos).use { out -> kryoAndroid(attachedContext).writeClassAndObject(out, continuation) }

            continuation = baos.toByteArray()
        }
        this.attachedContext = null
    }

    /**
     * Helper for [Parcelable]
     * @see [Parcelable.Creator]
     */
    companion object CREATOR : Parcelable.Creator<ParcelableContinuation<Any?>> {
        override fun createFromParcel(parcel: Parcel): ParcelableContinuation<Any?> {
            return ParcelableContinuation(parcel)
        }

        override fun newArray(size: Int): Array<ParcelableContinuation<Any?>?> {
            return arrayOfNulls(size)
        }

        @JvmStatic
        private val TAG = ParcelableContinuation::class.java.simpleName
    }
}