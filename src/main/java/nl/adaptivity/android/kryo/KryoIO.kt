@file:Suppress("unused")

package nl.adaptivity.android.kryo

import android.app.Application
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Registration
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.serializers.FieldSerializer
import com.esotericsoftware.kryo.serializers.FieldSerializerConfig
import com.esotericsoftware.kryo.util.DefaultClassResolver
import com.esotericsoftware.kryo.util.MapReferenceResolver
import kotlinx.coroutines.experimental.CommonPool
import org.objenesis.instantiator.ObjectInstantiator
import org.objenesis.strategy.InstantiatorStrategy
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import kotlin.coroutines.experimental.intrinsics.COROUTINE_SUSPENDED
import org.objenesis.strategy.StdInstantiatorStrategy



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

private class ContextSerializer(private val context: Context?) : Serializer<android.content.Context>() {

    override fun read(kryo: Kryo, input: Input, type: Class<Context>): Context? {
        val result: Context? = when (kryo.readObject(input, KryoAndroidConstants::class.java)) {
            KryoAndroidConstants.CONTEXT -> type.cast(context)
            KryoAndroidConstants.APPLICATIONCONTEXT -> type.cast(context?.applicationContext)
            else -> null
        }
        return result?.also { kryo.reference(it) }
    }

    override fun write(kryo: Kryo, output: Output, obj: Context?) {
        when (obj) {
            is Context -> kryo.writeObject(output, KryoAndroidConstants.CONTEXT)
            is Application -> kryo.writeObject(output, KryoAndroidConstants.APPLICATIONCONTEXT)
            else -> throw IllegalArgumentException("Serializing contexts only works for activity, application and service")
        }

    }
}

enum class KryoAndroidConstants {
    UNDECIDED,
    RESUMED,
    COROUTINE_SUSPENDED,
    APPLICATIONCONTEXT,
    CONTEXT
}

private class InitialResultSerializer(val parent: Serializer<Any?>): Serializer<Any?>() {
    override fun read(kryo: Kryo, input: Input, type: Class<Any?>): Any? {
        val readValue = kryo.readClassAndObject(input)
        return when (readValue) {
            KryoAndroidConstants.COROUTINE_SUSPENDED -> COROUTINE_SUSPENDED
            KryoAndroidConstants.RESUMED -> _Resumed
            KryoAndroidConstants.UNDECIDED -> _Undecided
            else -> readValue
        }
    }

    override fun write(kryo: Kryo, output: Output, obj: Any?) {
        when (obj) {
            COROUTINE_SUSPENDED -> kryo.writeClassAndObject(output, KryoAndroidConstants.COROUTINE_SUSPENDED)
            _Resumed -> kryo.writeClassAndObject(output, KryoAndroidConstants.RESUMED)
            _Undecided -> kryo.writeClassAndObject(output, KryoAndroidConstants.UNDECIDED)
            else -> parent.write(kryo, output, obj)
        }
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
private val _Resumed = _SafeContinuation.getDeclaredField("RESUMED").let { f -> f.isAccessible=true; f.get(null) }
private val _Undecided = _SafeContinuation.getDeclaredField("UNDECIDED").let { f -> f.isAccessible=true; f.get(null) }

private class SafeContinuationSerializer(kryo: Kryo): FieldSerializer<Any>(kryo, _SafeContinuation) {

    override fun write(kryo: Kryo, output: Output, obj: Any?) {
        val resultField = getField("result").field.apply { isAccessible=true }
        val resultValue = resultField.get(obj)
        var changed = true
        // If the result field is one of the special objects, map them to the enum instances for
        // safe serialization
        when (resultValue) {
            COROUTINE_SUSPENDED -> resultField.set(obj, KryoAndroidConstants.COROUTINE_SUSPENDED)
            _Resumed -> resultField.set(obj, KryoAndroidConstants.RESUMED)
            _Undecided -> resultField.set(obj, KryoAndroidConstants.UNDECIDED)
            else -> changed = false
        }
        super.write(kryo, output, obj)
        // Undo the changes
        if (changed) {
            resultField.set(obj, resultValue)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun read(kryo: Kryo, input: Input, type: Class<Any>): Any? {
        val obj = super.read(kryo, input, type)
        val resultField = getField("result").field.apply { isAccessible=true }
        val resultValue = resultField.get(obj)
        when (resultValue) {
            KryoAndroidConstants.COROUTINE_SUSPENDED -> resultField.set(obj, COROUTINE_SUSPENDED)
            KryoAndroidConstants.RESUMED -> resultField.set(obj, _Resumed)
            KryoAndroidConstants.UNDECIDED -> resultField.set(obj, _Undecided)
        }

        return obj
    }
}

/*
open class DataSerializer(kryo: Kryo, type: Class<*>): FieldSerializer<Any>(kryo, type/*, null, FieldSerializerConfig().apply { isIgnoreSyntheticFields=true }*/) {
    override fun write(kryo: Kryo, output: Output, obj: Any?) {
        super.write(kryo, output, obj)
    }

    override fun create(kryo: Kryo, input: Input, type: Class<Any>): Any {
        return allocate(type)
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
        try {
            return type.declaredConstructors.first().apply { isAccessible=true }.newInstance(*params.toTypedArray())
        } catch (e: Exception) {
            val message = buildString {
                append("Error creating ").append(type.name).append(": ").appendln(e.message)
                for(f in fields) {
                    val fld = f.field
                    append("  field: ").append(fld.declaringClass.name).append('#').append(fld.name).append(": ").appendln(fld.type.name)
                }
                type.declaredConstructors.first().parameterTypes.joinTo(this, prefix = "  <init>(", postfix = ")") { it.name }
            }
            throw IllegalArgumentException(message, e)
        }
    }

    companion object {
        val unsafe = Class.forName("sun.misc.Unsafe").getMethod("getUnsafe").invoke(null)
        private val allocateInstance = unsafe::class.java.getDeclaredMethod("allocateInstance", Class::class.java)
        inline fun <reified T> allocate() = allocate(T::class.java)
        @Suppress("UNCHECKED_CAST")
        fun <T> allocate(t:Class<T>)  =  allocateInstance.invoke(unsafe, t) as T
    }
}
*/

private class StandaloneCoroutineSerializer(kryo: Kryo, type: Class<*>): FieldSerializer<Any>(kryo, type) {
    val _parentContext: CachedField<*> = fields.first { it.field.declaringClass==type && it.field.name=="parentContext" }.also { removeField(it) }

    override fun create(kryo: Kryo, input: Input, type: Class<Any>): Any {
        val parentContext = kryo.readClassAndObject(input)
        return type.constructors.first().apply { isAccessible=true }.newInstance(parentContext, true)
    }

    override fun write(kryo: Kryo, output: Output, obj: Any) {
        _parentContext.write(output, obj)

        super.write(kryo, output, obj)
    }
}

private class ObjectSerializer(kryo: Kryo, val type: Class<*>): Serializer<Any>(true, true) {
    override fun read(kryo: Kryo, input: Input, type: Class<Any>): Any {
        return type.fields.first { it.name=="INSTANCE" }.get(null)
    }

    override fun write(kryo: Kryo, output: Output, obj: Any?) {
//        kryo.writeClass(output, type)
    }
}

private class CoroutineImplSerializer(kryo: Kryo, type: Class<*>): FieldSerializer<Any>(kryo, type, null, FieldSerializerConfig().apply { isIgnoreSyntheticFields=false }) {
    override fun write(kryo: Kryo, output: Output, obj: Any?) {
        super.write(kryo, output, obj)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<Any>): Any {
        return super.read(kryo, input, type)
    }
}


val kryoAndroid get(): Kryo = Kryo(AndroidKotlinResolver(null), MapReferenceResolver()).apply { registerAndroidSerializers() }

fun kryoAndroid(context: Context): Kryo = Kryo(AndroidKotlinResolver(context), MapReferenceResolver()).apply { registerAndroidSerializers() }

fun Kryo.registerAndroidSerializers() {
    instantiatorStrategy = KotlinObjectInstantiatorStrategy(Kryo.DefaultInstantiatorStrategy(StdInstantiatorStrategy()))

    register(_SafeContinuation, SafeContinuationSerializer(this))
    register(CommonPool::class.java, ObjectSerializer(this, CommonPool::class.java))
}

class KotlinObjectInstantiatorStrategy(private val fallback: InstantiatorStrategy) : InstantiatorStrategy {

    class KotlinObjectInstantiator<T>(type: Class<T>): ObjectInstantiator<T> {
        @Suppress("UNCHECKED_CAST")
        private val INSTANCE = type.getField("INSTANCE").get(null) as T

        override fun newInstance() = INSTANCE
    }

    override fun <T : Any?> newInstantiatorOf(type: Class<T>): ObjectInstantiator<T> {
        if (type.constructors.isEmpty() && type.fields.any { it.name=="INSTANCE" }) {
            return KotlinObjectInstantiator(type)
        } else {
            return fallback.newInstantiatorOf(type)
        }
    }
}

class AndroidKotlinResolver(private val context: Context?) : DefaultClassResolver() {

    override fun getRegistration(type: Class<*>): Registration? {
        return when {
            type.isPrimitive || type==Any::class.java || type.superclass==null -> super.getRegistration(type)
            Context::class.java.isAssignableFrom(type.superclass) -> super.getRegistration(type) ?: run {
                register(Registration(type, ContextSerializer(context), -1))
            }
            type.superclass?.name=="kotlin.coroutines.experimental.jvm.internal.CoroutineImpl" -> {
                super.getRegistration(type)?:run {
                    register(Registration(type, CoroutineImplSerializer(kryo, type), -1))
                }
            }
            else -> super.getRegistration(type)
        }
    }

    companion object {
        const val TAG = "AndroidKotlinResolver"
    }
}