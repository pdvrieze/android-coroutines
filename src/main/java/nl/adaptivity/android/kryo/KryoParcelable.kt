package nl.adaptivity.android.kryo

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import java.io.ByteArrayOutputStream

/**
 * A [Parcelable] that can be stored using [Kryo]. For now it is hardcoded to use the Kryo
 * object created by [kryoAndroid].
 */
class KryoParcelable<out T: Any>(val data: T): Parcelable {

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeKryoObject(data)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<KryoParcelable<Any>> {
        override fun createFromParcel(parcel: Parcel): KryoParcelable<Any> {
            return KryoParcelable(parcel.readKryoObject(kryoAndroid))
        }

        override fun newArray(size: Int): Array<KryoParcelable<Any>?> {
            return arrayOfNulls(size)
        }
    }

}

inline fun <reified T> Parcel.readKryoObject(kryo: Kryo) =
        readKryoObject(T::class.java, kryo)

inline fun <reified T> Parcel.readKryoObject(context: Context) =
        readKryoObject(T::class.java, kryoAndroid(context))


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
 * things and know that we will not clobber the buffer.
 */
private class UnsafeByteArrayOutputStream : ByteArrayOutputStream() {
    fun buf(): ByteArray = buf
    fun count(): Int = count
}
