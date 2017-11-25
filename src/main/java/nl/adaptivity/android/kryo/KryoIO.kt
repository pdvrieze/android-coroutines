@file:Suppress("unused")

package nl.adaptivity.android.kryo

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Registration
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.util.DefaultClassResolver
import com.esotericsoftware.kryo.util.MapReferenceResolver
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

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
            return KryoSerializable(parcel.readKryoObject())
        }

        override fun newArray(size: Int): Array<KryoSerializable<Any>?> {
            return arrayOfNulls(size)
        }
    }


}

fun Parcel.writeKryoObject(obj: Any?, kryo: Kryo = Kryo()) {
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

inline fun <reified T> Parcel.readKryoObject(kryo: Kryo = Kryo()) =
        readKryoObject(T::class.java, kryo)

fun <T> Parcel.readKryoObject(type:Class<T>, kryo: Kryo = Kryo()): T {
    val size = readInt()
    @Suppress("UNCHECKED_CAST")
    if (size<=0) return null as T
    val input = ByteArray(size)
    return type.cast(kryo.readClassAndObject(Input(input)))
}


/**
 * Helper class that exposes the buf and count fields. Saves an array copy here when we can control
 * things
 */
private class UnsafeByteArrayOutputStream : ByteArrayOutputStream() {
    fun buf(): ByteArray = buf
    fun count(): Int = count
}

private object ContextSerializer : Serializer<Context>() {
    override fun read(kryo: Kryo?, input: Input?, type: Class<Context>?): Context? {
        return null
    }

    override fun write(kryo: Kryo?, output: Output?, `object`: Context?) {
        throw IllegalArgumentException("Serializing contexts is fundamentally unsound")
    }
}

val kryoNoContext get(): Kryo {
    return Kryo(AndroidKotlinResolver(), MapReferenceResolver())
}

class AndroidKotlinResolver(val contextAllowed: Boolean = false) : DefaultClassResolver() {
    private val contextRegistration = Registration(Context::class.java, ContextSerializer, -1)

    override fun getRegistration(type: Class<*>?): Registration {
        if (Context::class.java.isAssignableFrom(type)) return contextRegistration
        return super.getRegistration(type)
    }
}