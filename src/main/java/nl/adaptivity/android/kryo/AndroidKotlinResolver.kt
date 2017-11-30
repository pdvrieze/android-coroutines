package nl.adaptivity.android.kryo

import android.content.Context
import com.esotericsoftware.kryo.Registration
import com.esotericsoftware.kryo.util.DefaultClassResolver
import nl.adaptivity.android.kryo.serializers.ContextSerializer
import nl.adaptivity.android.kryo.serializers.CoroutineImplSerializer

class AndroidKotlinResolver(private val context: Context?) : DefaultClassResolver() {

    override fun getRegistration(type: Class<*>): Registration? {
        return when {
            type.isPrimitive || type==Any::class.java || type.superclass==null -> super.getRegistration(type)
            Context::class.java.isAssignableFrom(type.superclass) -> super.getRegistration(type) ?: run {
                register(Registration(type, ContextSerializer(context), -1))
            }
            type.superclass?.name=="kotlin.coroutines.experimental.jvm.internal.CoroutineImpl" -> {
                super.getRegistration(type)?:run {
                    register(Registration(type, CoroutineImplSerializer(kryo, type), -1))
                }
            }
            else -> super.getRegistration(type)
        }
    }

    companion object {
        const val TAG = "AndroidKotlinResolver"
    }
}