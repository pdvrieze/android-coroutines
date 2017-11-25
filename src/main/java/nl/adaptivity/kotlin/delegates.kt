package nl.adaptivity.kotlin

import java.lang.ref.WeakReference
import java.io.Serializable
import kotlin.reflect.KProperty

/**
 * Create a weak lazy property. This differs from a general [lazy] in that it uses a weak reference.
 */
fun <T> weakLazy(initializer: () -> T): WeakLazy<T> = SynchronizedWeakLazyImpl(initializer)

/**
 * Creates a new instance of the [WeakLazy] that uses the specified initialization function [initializer]
 * and thread-safety [mode].
 *
 * If the initialization of a value throws an exception, it will attempt to reinitialize the value at next access.
 *
 * Note that when the [LazyThreadSafetyMode.SYNCHRONIZED] mode is specified the returned instance uses itself
 * to synchronize on. Do not synchronize from external code on the returned instance as it may cause accidental deadlock.
 * Also this behavior can be changed in the future.
 */
fun <T> weakLazy(mode: LazyThreadSafetyMode, initializer: () -> T): WeakLazy<T> =
        when (mode) {
            LazyThreadSafetyMode.SYNCHRONIZED -> SynchronizedWeakLazyImpl(initializer)
            LazyThreadSafetyMode.PUBLICATION -> SafePublicationWeakLazyImpl(initializer)
            LazyThreadSafetyMode.NONE -> UnsafeWeakLazyImpl(initializer)
        }

/**
 * Creates a new instance of the [WeakLazy] that uses the specified initialization function [initializer]
 * and the default thread-safety mode [LazyThreadSafetyMode.SYNCHRONIZED].
 *
 * If the initialization of a value throws an exception, it will attempt to reinitialize the value at next access.
 *
 * The returned instance uses the specified [lock] object to synchronize on.
 * When the [lock] is not specified the instance uses itself to synchronize on,
 * in this case do not synchronize from external code on the returned instance as it may cause accidental deadlock.
 * Also this behavior can be changed in the future.
 */
fun <T> weakLazy(lock: Any?, initializer: () -> T): WeakLazy<T> = SynchronizedWeakLazyImpl(initializer, lock)

/**
 * An extension to delegate a read-only property of type [T] to an instance of [Lazy].
 *
 * This extension allows to use instances of Lazy for property delegation:
 * `val property: String by lazy { initializer }`
 */
@Suppress("NOTHING_TO_INLINE")
inline operator fun <T> WeakLazy<T>.getValue(thisRef: Any?, property: KProperty<*>): T = value

/**
 * Represents a value with lazy initialization.
 *
 * To create an instance of [Lazy] use the [lazy] function.
 */
interface WeakLazy<out T> {
    /**
     * Gets the lazily initialized value of the current Lazy instance.
     * Once the value was initialized it must not change during the rest of lifetime of this Lazy instance.
     */
    val value: T

    /**
     * Returns `true` if a value for this Lazy instance has been already initialized, and `false` otherwise.
     * Once this function has returned `true` it stays `true` for the rest of lifetime of this Lazy instance.
     */
    fun isInitialized(): Boolean
}

private object NULL_VALUE

private class SynchronizedWeakLazyImpl<out T>(initializer: () -> T, lock: Any? = null) : WeakLazy<T>, Serializable {
    private var initializer: (() -> T)? = initializer
    @Volatile private var _value: WeakReference<Any?>? = null
    // final field is required to enable safe publication of constructed instance
    private val lock = lock ?: this

    override val value: T
        get() {
            val _v1 = _value?.get()
            @Suppress("UNCHECKED_CAST")
            when {
                _v1 == NULL_VALUE -> return null as T
                _v1 != null -> return _v1 as T
            }

            return synchronized(lock) {
                val _v2 = _value?.get()
                @Suppress("UNCHECKED_CAST")
                when {
                    _v2 == NULL_VALUE -> null as T
                    _v2 != null -> _v2 as T
                    else -> {
                        val typedValue = initializer!!()
                        _value = WeakReference(typedValue ?: NULL_VALUE)
                        initializer = null
                        typedValue
                    }
                }
            }
        }

    override fun isInitialized(): Boolean = _value?.get() != null

    override fun toString(): String = if (isInitialized()) value.toString() else "Lazy value not initialized yet."

    private fun writeReplace(): Any = InitializedWeakLazyImpl(value)
}

// internal to be called from lazy in JS
internal class UnsafeWeakLazyImpl<out T>(initializer: () -> T) : WeakLazy<T>, Serializable {
    private var initializer: (() -> T)? = initializer
    private var _value: WeakReference<Any?>? = null

    override val value: T
        get() {
            val _v = _value?.get()
            if (_v == null) {
                val value = initializer!!()
                _value = WeakReference(value ?: NULL_VALUE)
                return value
            }
            @Suppress("UNCHECKED_CAST")
            return (_v.let { if (it == NULL_VALUE) null else it }) as T
        }

    override fun isInitialized(): Boolean = _value?.get() != null

    override fun toString(): String = if (isInitialized()) value.toString() else "Lazy value not initialized yet."

    private fun writeReplace(): Any = InitializedWeakLazyImpl(value)
}

private class InitializedWeakLazyImpl<out T>(override val value: T) : WeakLazy<T>, Serializable {

    override fun isInitialized(): Boolean = true

    override fun toString(): String = value.toString()

}

private class SafePublicationWeakLazyImpl<out T>(private val initializer: () -> T) : WeakLazy<T>, Serializable {
    @Volatile private var _value: WeakReference<Any?>? = null
    // this final field is required to enable safe publication of constructed instance
    private val final: Any = NULL_VALUE

    override val value: T
        get() {
            @Suppress("CanBeVal")
            var initialValue = _value?.get()
            if (initialValue == null) {
                val newValue = initializer()
                if (_value == null) {
                    if(valueUpdater.compareAndSet(this, null, WeakReference(newValue ?: NULL_VALUE))) {
                        return newValue
                    }
                } else {
                    if(valueUpdater.compareAndSet(this, initialValue, WeakReference(newValue ?: NULL_VALUE))) {
                        return newValue
                    }
                    // weak reference dropped
                }
                @Suppress("RecursivePropertyAccessor")
                return value /* recursively call itself, but both new and initial values are in handle so
                                nothing should have been garbage collected.*/
            }
            @Suppress("UNCHECKED_CAST")
            return (if(initialValue==NULL_VALUE) null else initialValue) as T
        }

    override fun isInitialized(): Boolean = _value?.get() != null

    override fun toString(): String = if (isInitialized()) value.toString() else "Lazy value not initialized yet."

    private fun writeReplace(): Any = InitializedWeakLazyImpl(value)

    companion object {
        private val valueUpdater = java.util.concurrent.atomic.AtomicReferenceFieldUpdater.newUpdater(
                SafePublicationWeakLazyImpl::class.java,
                Any::class.java,
                "_value")
    }
}