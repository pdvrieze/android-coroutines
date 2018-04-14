package nl.adaptivity.android.kryo.serializers

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import kotlin.coroutines.experimental.intrinsics.COROUTINE_SUSPENDED

internal class InitialResultSerializer(val parent: Serializer<Any?>): Serializer<Any?>() {
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

