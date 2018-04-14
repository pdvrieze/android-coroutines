package nl.adaptivity.android.kryo.serializers

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.serializers.FieldSerializer

internal class StandaloneCoroutineSerializer(kryo: Kryo, type: Class<*>): FieldSerializer<Any>(kryo, type) {
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