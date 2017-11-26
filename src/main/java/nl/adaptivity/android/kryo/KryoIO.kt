@file:Suppress("unused")

package nl.adaptivity.android.kryo

import android.app.Activity
import android.app.Application
import android.app.Service
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Registration
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.serializers.FieldSerializer
import com.esotericsoftware.kryo.util.DefaultClassResolver
import com.esotericsoftware.kryo.util.MapReferenceResolver
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.intrinsics.COROUTINE_SUSPENDED

/**
 * Input class that uses a parcel to serialize. Perhaps not sustainable and ByteArrayStreams are better.
 */
class ParcelInput : Input {

    private val parcel: Parcel

    @JvmOverloads
    constructor(parcel: Parcel, bufferSize: Int = DEFAULT_BUFFER) : super(bufferSize) { this.parcel = parcel }
    constructor(parcel: Parcel, buffer: ByteArray?) : super(buffer) { this.parcel = parcel }
    constructor(parcel: Parcel, buffer: ByteArray?, offset: Int, count: Int) : super(buffer, offset, count) { this.parcel = parcel }


    override fun setInputStream(inputStream: InputStream?) {
        throw UnsupportedOperationException("ParcelInput reads from parcels, not streams")
    }

    override fun fill(buffer: ByteArray, offset: Int, count: Int): Int {
        val realCount = minOf(count, parcel.dataAvail())
        for (i in offset until realCount) {
            buffer[i] = parcel.readByte()
        }
        return realCount
    }

    override fun available(): Int {
        return limit - position + parcel.dataAvail()
    }

    override fun close() {
        // Don't do anything for now. Don't do our own recycling here.
    }

    companion object {
        const val DEFAULT_BUFFER = 1024
    }
}

/**
 * Output class that uses a parcel to serialize. Perhaps not sustainable and ByteArrayStreams are better.
 */
class ParcelOutput: Output {
    private val parcel: Parcel

    @JvmOverloads
    constructor(parcel: Parcel, bufferSize: Int = DEFAULT_BUFFER) : super(bufferSize) { this.parcel = parcel }
    constructor(parcel: Parcel, bufferSize: Int, maxBufferSize: Int) : super(bufferSize, maxBufferSize) { this.parcel = parcel }
    constructor(parcel: Parcel, buffer: ByteArray?) : super(buffer) { this.parcel = parcel }
    constructor(parcel: Parcel, buffer: ByteArray?, maxBufferSize: Int) : super(buffer, maxBufferSize) { this.parcel = parcel }

    override fun setOutputStream(outputStream: OutputStream) {
        throw UnsupportedOperationException("ParcelInput writes to parcels, not streams")
    }

    override fun flush() {
        for(i in 0 until position) {
            parcel.writeByte(buffer[i])
        }
    }

    override fun close() {
        // Do nothing
    }

    companion object {
        const val DEFAULT_BUFFER = 1024
    }
}

/**
 * Helper object that independently allows objects to be parcelled using Kryo
 */
class KryoSerializable<out T: Any>(val data: T): Parcelable {

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeKryoObject(data)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<KryoSerializable<Any>> {
        override fun createFromParcel(parcel: Parcel): KryoSerializable<Any> {
            return KryoSerializable(parcel.readKryoObject(kryoAndroid))
        }

        override fun newArray(size: Int): Array<KryoSerializable<Any>?> {
            return arrayOfNulls(size)
        }
    }


}

fun Parcel.writeKryoObject(obj: Any?, kryo: Kryo = kryoAndroid) {
    if (obj==null) {
        writeInt(-1)
    } else {
        val baos = UnsafeByteArrayOutputStream()
        Output(baos).use { output ->
            kryo.writeClassAndObject(output, obj)
        }
        writeInt(baos.count())
        writeByteArray(baos.buf(), 0, baos.count())
    }
}

inline fun <reified T> Parcel.readKryoObject(kryo: Kryo) =
        readKryoObject(T::class.java, kryo)

inline fun <reified T> Parcel.readKryoObject(context: Context) =
        readKryoObject(T::class.java, kryoAndroid(context))

inline fun <T> Parcel.readKryoObject(type:Class<T>, context: Context) = readKryoObject(type, kryoAndroid(context))

fun <T> Parcel.readKryoObject(type:Class<T>, kryo: Kryo): T {
    val size = readInt()
    @Suppress("UNCHECKED_CAST")
    if (size<=0) return null as T
    val input = ByteArray(size)
    val kryoValue = kryo.readClassAndObject(Input(input))
    return type.cast(kryoValue)
}


/**
 * Helper class that exposes the buf and count fields. Saves an array copy here when we can control
 * things
 */
private class UnsafeByteArrayOutputStream : ByteArrayOutputStream() {
    fun buf(): ByteArray = buf
    fun count(): Int = count
}

private class ContextSerializer(private val context: Context) : Serializer<Context>() {

    enum class CONTEXT_STORAGE { APPLICATIONCONTEXT, ACTIVITY, SERVICE }

    override fun read(kryo: Kryo, input: Input, type: Class<Context>): Context? {
        return when (kryo.readObject(input, CONTEXT_STORAGE::class.java)) {
            CONTEXT_STORAGE.APPLICATIONCONTEXT -> context as Application
            CONTEXT_STORAGE.ACTIVITY -> context as Activity
            CONTEXT_STORAGE.SERVICE -> context as Service
            null -> null
        }
    }

    override fun write(kryo: Kryo, output: Output, obj: Context?) {
        when (obj) {
            is Activity -> kryo.writeObject(output, CONTEXT_STORAGE.ACTIVITY )
            is Application -> kryo.writeObject(output, CONTEXT_STORAGE.APPLICATIONCONTEXT)
            is Service -> kryo.writeObject(output, CONTEXT_STORAGE.SERVICE)
            else -> throw IllegalArgumentException("Serializing contexts only works for activity, application and service")
        }

    }
}

enum class ContinuationValues {
    UNDECIDED,
    RESUMED,
    COROUTINE_SUSPENDED
}

private class InitialResultSerializer(val parent: Serializer<Any?>): Serializer<Any?>() {
    override fun read(kryo: Kryo?, input: Input?, type: Class<Any?>?): Any? {
        val readValue = parent.read(kryo, input, type)
        return when (readValue) {
            ContinuationValues.COROUTINE_SUSPENDED -> COROUTINE_SUSPENDED
            ContinuationValues.RESUMED -> _Resumed
            ContinuationValues.UNDECIDED -> _Undecided
            else -> readValue
        }
    }

    override fun write(kryo: Kryo?, output: Output?, obj: Any?) {
        val toWrite = when (obj) {
            COROUTINE_SUSPENDED -> ContinuationValues.COROUTINE_SUSPENDED
            _Resumed -> ContinuationValues.RESUMED
            _Undecided -> ContinuationValues.UNDECIDED
            else -> obj
        }
        parent.write(kryo, output, toWrite)
    }

    override fun isImmutable(): Boolean = parent.isImmutable()

    override fun setImmutable(immutable: Boolean) = parent.setImmutable(immutable)

    override fun setAcceptsNull(acceptsNull: Boolean) = parent.setAcceptsNull(acceptsNull)

    override fun copy(kryo: Kryo?, original: Any?): Any? = parent.copy(kryo, original)

    override fun getAcceptsNull(): Boolean = parent.getAcceptsNull()

    override fun setGenerics(kryo: Kryo?, generics: Array<out Class<Any>>?) =
            parent.setGenerics(kryo, generics)
}

private val _SafeContinuation = Class.forName("kotlin.coroutines.experimental.SafeContinuation")
private val _DispatchedContinuation = Class.forName("kotlinx.coroutines.experimental.DispatchedContinuation")
private val _CoroutineImpl = Class.forName("kotlin.coroutines.experimental.jvm.internal.CoroutineImpl")
private val _Resumed = _SafeContinuation.getDeclaredField("RESUMED").let { f -> f.isAccessible=true; f.get(null) }
private val _Undecided = _SafeContinuation.getDeclaredField("UNDECIDED").let { f -> f.isAccessible=true; f.get(null) }


class SafeContinuationSerializer(kryo: Kryo): FieldSerializer<Continuation<Any>>(kryo, _SafeContinuation) {
    override fun initializeCachedFields() {
        getField("result").serializer = InitialResultSerializer(kryo.getDefaultSerializer(Any::class.java))
    }

    @Suppress("UNCHECKED_CAST")
    override fun read(kryo: Kryo, input: Input, type: Class<Continuation<Any>>?): Continuation<Any> {
        val delegate = kryo.readClassAndObject(input)
        val initialResult = getField("result").serializer.read(kryo, input, Any::class.java)
        val obj = _SafeContinuation.constructors.first { it.parameterTypes.size == 2  && it.parameterTypes[0] == Continuation::class.java }.newInstance(delegate, initialResult)

        kryo.reference(obj)
        return obj as Continuation<Any>
    }
}

class DispatchedContinuationSerializer(kryo: Kryo): FieldSerializer<Continuation<Any>>(kryo, _DispatchedContinuation) {

    @Suppress("UNCHECKED_CAST")
    override fun read(kryo: Kryo, input: Input, type: Class<Continuation<Any>>?): Continuation<Any> {
        val dispatcher = kryo.readClassAndObject(input)
        val continuation = kryo.readClassAndObject(input)
        val obj = _SafeContinuation.constructors.first().newInstance(dispatcher, continuation)
        kryo.reference(obj)
        return obj as Continuation<Any>
    }
}

class CoroutineSerializer(kryo: Kryo): FieldSerializer<Any>(kryo, _CoroutineImpl) {
    override fun write(kryo: Kryo?, output: Output?, obj: Any?) {
        super.write(kryo, output, obj)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<Any>): Any {
        val params = fields.map { field ->  when (field.field.type) {
            java.lang.Integer.TYPE -> input.readInt()
            java.lang.Float.TYPE -> input.readFloat()
            java.lang.Short.TYPE -> input.readShort()
            java.lang.Byte.TYPE -> input.readByte()
            java.lang.Boolean.TYPE -> input.readBoolean()
            java.lang.Character.TYPE -> input.readChar()
            java.lang.Long.TYPE -> input.readLong()
            java.lang.Double.TYPE -> input.readDouble()
            else -> kryo.readClassAndObject(input)

        }}
        return type.constructors.first().newInstance(params.toTypedArray())
    }
}

val kryoAndroid get(): Kryo = Kryo(AndroidKotlinResolver(null), MapReferenceResolver()).apply { registerAndroidSerializers(null) }

fun kryoAndroid(context: Context): Kryo = Kryo(AndroidKotlinResolver(context), MapReferenceResolver()).apply { registerAndroidSerializers(context) }

fun Kryo.registerAndroidSerializers(context: Context?) {
    register(_SafeContinuation, SafeContinuationSerializer(this))
    register(_DispatchedContinuation, DispatchedContinuationSerializer(this))
    if (context!=null) register(Context::class.java, ContextSerializer(context))
    register(_CoroutineImpl, CoroutineSerializer(this))
}

class AndroidKotlinResolver(context: Context?) : DefaultClassResolver() {

    override fun getRegistration(type: Class<*>): Registration? {
        return when {
            Context::class.java.isAssignableFrom(type) -> getRegistration(Context::class.java)
            type.superclass?.name=="kotlin.coroutines.experimental.jvm.internal.CoroutineImpl" -> super.getRegistration(type.superclass)
            else -> super.getRegistration(type)
        }
    }
}