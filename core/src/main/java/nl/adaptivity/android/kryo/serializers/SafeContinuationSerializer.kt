package nl.adaptivity.android.kryo.serializers

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.serializers.FieldSerializer
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED

internal class SafeContinuationSerializer(kryo: Kryo): FieldSerializer<Any>(kryo, _SafeContinuation) {

/*
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
*/

/*
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
*/
}

@Suppress("ObjectPropertyName")
internal val _SafeContinuation = Class.forName("kotlin.coroutines.SafeContinuation")
/*
@Suppress("ObjectPropertyName")
internal val _Resumed = _SafeContinuation.getDeclaredField("RESUMED").let { f -> f.isAccessible=true; f.get(null) }
@Suppress("ObjectPropertyName")
internal val _Undecided = _SafeContinuation.getDeclaredField("UNDECIDED").let { f -> f.isAccessible=true; f.get(null) }
*/
