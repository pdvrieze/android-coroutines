package nl.adaptivity.android.kryo

import android.content.Context
import com.esotericsoftware.kryo.Registration
import com.esotericsoftware.kryo.util.DefaultClassResolver
import nl.adaptivity.android.kryo.serializers.ContextSerializer
import nl.adaptivity.android.kryo.serializers.CoroutineImplSerializer
import nl.adaptivity.android.kryo.serializers.ObjectSerializer
import java.lang.reflect.Modifier

class AndroidKotlinResolver(private val context: Context?) : DefaultClassResolver() {

    override fun getRegistration(type: Class<*>): Registration? {
        val superReg = super.getRegistration(type)
        return when {
            superReg!=null -> superReg
            type.superclass==null -> superReg
            Context::class.java.isAssignableFrom(type.superclass) ->
                register(Registration(type, ContextSerializer(context), NAME))
            type.superclass?.name=="kotlin.coroutines.experimental.jvm.internal.CoroutineImpl" ->
                register(Registration(type, CoroutineImplSerializer(kryo, type), NAME))
            type.isKObject -> register(Registration(type, ObjectSerializer(kryo, type), NAME))
            else -> null
        }
    }

    companion object {
        const val TAG = "AndroidKotlinResolver"
        const val NAME = DefaultClassResolver.NAME.toInt()
    }
}