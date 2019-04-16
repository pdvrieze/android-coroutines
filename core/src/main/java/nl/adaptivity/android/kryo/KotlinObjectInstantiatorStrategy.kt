package nl.adaptivity.android.kryo

import org.objenesis.instantiator.ObjectInstantiator
import org.objenesis.strategy.InstantiatorStrategy
import java.lang.reflect.Modifier

class KotlinObjectInstantiatorStrategy(private val fallback: InstantiatorStrategy) : InstantiatorStrategy {

    class KotlinObjectInstantiator<T>(type: Class<T>): ObjectInstantiator<T> {
        @Suppress("UNCHECKED_CAST")
        private val objectInstance = type.getField("INSTANCE").get(null) as T

        override fun newInstance() = objectInstance
    }

    override fun <T : Any?> newInstantiatorOf(type: Class<T>): ObjectInstantiator<T> {
        if (type.isKObject) {
            return KotlinObjectInstantiator(type)
        } else {
            return fallback.newInstantiatorOf(type)
        }
    }
}

internal val Class<*>.isKObject: Boolean get() {
    return Modifier.isFinal(modifiers) && constructors.isEmpty() && fields.any { it.name=="INSTANCE" && Modifier.isStatic(it.modifiers) }
}