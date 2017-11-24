package nl.adaptivity.android.kotlin

import android.content.Context
import java.lang.ref.WeakReference
import kotlin.reflect.KProperty

inline val <C:Context> C.weakRef get() = WeakReference<C>(this)

@Suppress("NOTHING_TO_INLINE")
inline operator fun <T> WeakReference<T>.getValue(thisReference: Nothing?, prop: KProperty<*>):T? = this.get()