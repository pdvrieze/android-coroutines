package nl.adaptivity.android.kryo.serializers

import android.app.Application
import android.content.Context
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output

internal class ContextSerializer(private val context: Context?) : Serializer<Context>() {

    override fun read(kryo: Kryo, input: Input, type: Class<Context>): Context? {
        val result: Context? = when (kryo.readObject(input, KryoAndroidConstants::class.java)) {
            KryoAndroidConstants.CONTEXT -> {
                val savedContextType = kryo.readClass(input).type
                if (! type.isAssignableFrom(savedContextType)) {
                    throw ClassCastException("Saved a context of type ${savedContextType}, but asked to inflate as ${type}")
                }
                type.cast(context)
            }
            KryoAndroidConstants.APPLICATIONCONTEXT -> type.cast(context?.applicationContext)
            else -> null
        }
        return result?.also { kryo.reference(it) }
    }

    override fun write(kryo: Kryo, output: Output, obj: Context?) {
        when (obj) {
            is Context -> {
                kryo.writeObject(output, KryoAndroidConstants.CONTEXT)
                kryo.writeClass(output, obj.javaClass)
            }
            is Application -> kryo.writeObject(output, KryoAndroidConstants.APPLICATIONCONTEXT)
            else -> throw IllegalArgumentException("Serializing contexts only works for activity, application and service")
        }

    }
}