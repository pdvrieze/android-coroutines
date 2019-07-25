package nl.adaptivity.android.kryo.serializers

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.serializers.FieldSerializer
import com.esotericsoftware.kryo.serializers.FieldSerializerConfig

internal class ContinuationImplSerializer(kryo: Kryo, type: Class<*>): FieldSerializer<Any>(kryo, type, null, FieldSerializerConfig().apply { isIgnoreSyntheticFields=false }) {
    override fun write(kryo: Kryo, output: Output, obj: Any?) {
        super.write(kryo, output, obj)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<Any>): Any {
        return super.read(kryo, input, type)
    }
}