package nl.adaptivity.android.kryo.serializers

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import nl.adaptivity.android.kryo.serializers.ReferenceType.*
import java.lang.ref.Reference
import java.lang.ref.SoftReference
import java.lang.ref.WeakReference

/**
 * Serializer for Kotlin objects that stores nothing and just retrieves the current instance from
 * the field.
 */
internal class ReferenceSerializer(kryo: Kryo, val type: Class<out Reference<*>>): Serializer<Reference<*>>(false, true) {
    /**
     * The correct way of getting an object is getting it's instance.
     */
    override fun read(kryo: Kryo, input: Input, type: Class<Reference<*>>): Reference<*> {
        val result = when (kryo.readObject(input, ReferenceType::class.java)) {
            SOFTREF -> SoftReference(null)
            WEAKREF -> WeakReference(null)
            else -> throw IllegalArgumentException("Unsupported reference")
        }
        return result
    }

    override fun write(kryo: Kryo, output: Output, obj: Reference<*>?) {
        val substitute = when (obj) {
            is SoftReference<*> -> SOFTREF
            is WeakReference<*> -> WEAKREF
            else -> IllegalArgumentException("Serializing contexts only works for Soft and Weak references")
        }
        kryo.writeObject(output, substitute)
        // The class is already written by the caller so no need to write anything
    }
}

private enum class ReferenceType {
    SOFTREF,
    WEAKREF
}