@file:Suppress("NOTHING_TO_INLINE")

package nl.adaptivity.android.kotlin

import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Parcelable
import android.support.annotation.RequiresApi
import android.util.Size
import android.util.SizeF
import android.util.SparseArray
import java.io.Serializable
import java.util.ArrayList

inline fun bundle(capacity:Int, configurator: (Bundle) -> Unit) = Bundle(capacity).apply(configurator)

inline fun bundle(configurator: (Bundle) -> Unit) = Bundle().apply(configurator)

operator fun Bundle.set(key:String, value: String) = putString(key, value)
operator fun Bundle.set(key:String, value: Int) = putInt(key, value)

@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
inline operator fun Bundle.set(key:String, value: IBinder) = putBinder(key, value)
inline operator fun Bundle.set(key:String, value: Bundle) = putBundle(key, value)
inline operator fun Bundle.set(key:String, value: Byte) = putByte(key, value)
inline operator fun Bundle.set(key:String, value: ByteArray) = putByteArray(key, value)
inline operator fun Bundle.set(key:String, value: Char) = putChar(key, value)
inline operator fun Bundle.set(key:String, value: CharArray) = putCharArray(key, value)
inline operator fun Bundle.set(key:String, value: CharSequence) = putCharSequence(key, value)
inline operator fun Bundle.set(key:String, value: Array<CharSequence>) = putCharSequenceArray(key, value)
@JvmName("setCharsequenceArrayList")
inline operator fun Bundle.set(key:String, value: ArrayList<CharSequence>) = putCharSequenceArrayList(key, value)
inline operator fun Bundle.set(key:String, value: Float) = putFloat(key, value)
inline operator fun Bundle.set(key:String, value: FloatArray) = putFloatArray(key, value)
@JvmName("setIntegerArrayList")
inline operator fun Bundle.set(key:String, value: ArrayList<Int>) = putIntegerArrayList(key, value)
inline operator fun Bundle.set(key:String, value: Parcelable) = putParcelable(key, value)
inline operator fun Bundle.set(key:String, value: Array<Parcelable>) = putParcelableArray(key, value)
@JvmName("setParcelableArrayList")
inline operator fun Bundle.set(key:String, value: ArrayList<out Parcelable>) = putParcelableArrayList(key, value)
inline operator fun Bundle.set(key:String, value: Serializable) = putSerializable(key, value)
inline operator fun Bundle.set(key:String, value: Short) = putShort(key, value)
inline operator fun Bundle.set(key:String, value: ShortArray) = putShortArray(key, value)
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
inline operator fun Bundle.set(key:String, value: Size) = putSize(key, value)
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
inline operator fun Bundle.set(key:String, value: SizeF) = putSizeF(key, value)
inline operator fun Bundle.set(key:String, value: SparseArray<out Parcelable>) = putSparseParcelableArray(key, value)
@JvmName("setStringArrayList")
inline operator fun Bundle.set(key:String, value: ArrayList<String>) = putStringArrayList(key, value)
