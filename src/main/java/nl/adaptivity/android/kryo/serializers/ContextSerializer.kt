package nl.adaptivity.android.kryo.serializers

import android.app.Application
import android.content.Context
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import nl.adaptivity.android.kryo.serializers.KryoAndroidConstants.*

internal class ContextSerializer(private val context: Context?) : Serializer<Context>() {

    override fun read(kryo: Kryo, input: Input, type: Class<Context>): Context? {
        val result: Context? = when (kryo.readObject(input, KryoAndroidConstants::class.java)) {
            CONTEXT -> {
                val savedContextType = kryo.readClass(input).type
                if (! type.isAssignableFrom(savedContextType)) {
                    throw ClassCastException("Saved a context of type ${savedContextType}, but asked to inflate as ${type}")
                }
                type.cast(context)
            }
            APPLICATIONCONTEXT -> type.cast(context?.applicationContext)
            OTHERCONTEXT -> kryo.readClassAndObject(input) as Context?
            else -> null
        }
        return result?.also { kryo.reference(it) }
    }

    override fun write(kryo: Kryo, output: Output, obj: Context) {
        when (obj) {
            context -> {
                kryo.writeObject(output, CONTEXT)
                kryo.writeClass(output, obj.javaClass)
            }
            is Application -> kryo.writeObject(output, APPLICATIONCONTEXT)
            else -> {
                throw IllegalArgumentException("Attempting to serialize context of type ${obj.javaClass}")
                kryo.writeObject(output, OTHERCONTEXT)
                kryo.writeClassAndObject(output, obj)
            }
//            else -> throw IllegalArgumentException("Serializing contexts only works for activity, application and service")
        }

    }
}