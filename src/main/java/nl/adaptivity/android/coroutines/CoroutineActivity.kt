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
import nl.adaptivity.android.kryo.kryoAndroid
import nl.adaptivity.android.kryo.writeKryoObject
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * Extension method for activity that invokes [Activity.startActivityForResult] and invokes the
 * callback in [body] when complete. For use in Kotlin consider [activityResult] as a suspending
 * function instead.
 *
 * @receiver The activity that is extended.
 * @param intent The intent to invoke.
 * @param body The callback invoked on completion.
 */
fun <A:Activity> A.withActivityResult(intent: Intent, body: A.(ActivityResult)->Unit) {
    // Horrible hack to fix generics
    @Suppress("UNCHECKED_CAST")
    val contFragment = RetainedContinuationFragment(ParcelableContinuation(body, COROUTINEFRAGMENT_RESULTCODE_START))
    fragmentManager.beginTransaction().add(contFragment,RetainedContinuationFragment.TAG).commit()
    runOnUiThread {
        fragmentManager.executePendingTransactions()
        contFragment.startActivityForResult(intent, COROUTINEFRAGMENT_RESULTCODE_START)
    }
}

/**
 * Asynchronously invoke [Activity.startActivityForResult] returning the result on completion.
 */
suspend fun Activity.activityResult(intent:Intent): ActivityResult {
    return suspendCoroutine { continuation ->
        val fm = fragmentManager
        val contFragment = RetainedContinuationFragment(ParcelableContinuation(continuation, COROUTINEFRAGMENT_RESULTCODE_START))

        fm.beginTransaction().apply {
            // This shouldn't happen, but in that case remove the old continuation.
            fm.findFragmentByTag(RetainedContinuationFragment.TAG)?.let { remove(it) }

            add(contFragment, RetainedContinuationFragment.TAG)
        }.commit()


        runOnUiThread {
            fm.executePendingTransactions()
            contFragment.startActivityForResult(intent, COROUTINEFRAGMENT_RESULTCODE_START)
        }
    }
}

/**
 * Extension method for activity that invokes [Activity.startActivityForResult] and invokes the
 * callback in [body] when complete. For use in Kotlin consider [activityResult] as a suspending
 * function instead.
 *
 * @receiver The activity that is extended.
 * @param intent The intent to invoke.
 * @param options The options to pass on the activity start.
 * @param body The callback invoked on completion.
 */
@Suppress("unused")
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

/**
 * The starting (and for now only) result code that is used to start the activity. As it happens
 * from a special fragment the result code is actually ignored and should be safe from conflict.
 */
const val COROUTINEFRAGMENT_RESULTCODE_START = 0xf00

/**
 * The [Bundle] key under which the continuation is stored.
 */
const val KEY_ACTIVITY_CONTINUATION = "activityContinuation"

/**
 * Java compatibility interface to make the asynchronous use of [withActivityResult] with a callback
 * much friendlier.
 */
interface SerializableHandler<A, T> {
    operator fun invoke(activty: A, data:T)
}

/**
 * Java compatibility helper factory method
 */
@Suppress("FunctionName")
@JvmOverloads
fun <A:Activity, T> ParcelableContinuation(handler: SerializableHandler<A,T>, requestCode: Int = -1)
        = ParcelableContinuationCompat<A,T>({ handler(this, it) }, requestCode)

@Suppress("FunctionName")
fun <A:Activity, T> ParcelableContinuation(handler: A.(T) -> Unit, requestCode: Int = -1)
        = ParcelableContinuationCompat<A,T>({ handler(this, it) }, requestCode)

/**
 * [ParcelableContinuation] subclass that not only works with continuations, but also handles
 * Java and Kotlin callback lambdas.
 *
 * @param requestCode The request code that this continuation should resume on.
 * @param handlerOrContinuation The executable object that handles the result.
 */
class ParcelableContinuationCompat<A:Activity, T> private constructor(requestCode: Int, handlerOrContinuation: Any): ParcelableContinuation<T>(requestCode, handlerOrContinuation) {

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
    private fun resolve(context:Context): Any {
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
open class ParcelableContinuation<T> protected constructor(val requestCode: Int, protected var continuation: Any): Parcelable {

    /**
     * Create a new instance for the given handler.
     */
    constructor(handler: Continuation<T>, requestCode: Int = -1): this(requestCode, handler)


    /**
     * Read the continuation from the parcel. This will merely store the continuation data as a byte array for
     * Kryo to deserialize later. (Note that the parcel cannot be validly stored).
     */
    @Suppress("UNCHECKED_CAST")
    constructor(parcel: Parcel) :
            this(parcel.readInt(), continuation = ByteArray(parcel.readInt()).also { parcel.readByteArray(it) } ) {
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
                dest.writeKryoObject(h, kryoAndroid)
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
    private fun resolve(context:Context): Continuation<T> {
        val h = continuation
        @Suppress("UNCHECKED_CAST")
        return when (h) {
            is ByteArray -> (kryoAndroid(context).readClassAndObject(Input(h)) as Continuation<T>).also { continuation = it }
            else -> h as Continuation<T>
        }
    }

    override fun describeContents() = 0

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

typealias ActivityResult = Maybe<Intent?>

private fun RetainedContinuationFragment(activityContinuation: ParcelableContinuation<Maybe<Intent?>>) = RetainedContinuationFragment().also {
    it.arguments = Bundle(1).apply { putParcelable(KEY_ACTIVITY_CONTINUATION, activityContinuation) }
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