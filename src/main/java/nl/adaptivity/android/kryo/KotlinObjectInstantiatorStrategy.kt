package nl.adaptivity.android.kryo

import org.objenesis.instantiator.ObjectInstantiator
import org.objenesis.strategy.InstantiatorStrategy

class KotlinObjectInstantiatorStrategy(private val fallback: InstantiatorStrategy) : InstantiatorStrategy {

    class KotlinObjectInstantiator<T>(type: Class<T>): ObjectInstantiator<T> {
        @Suppress("UNCHECKED_CAST")
        private val INSTANCE = type.getField("INSTANCE").get(null) as T

        override fun newInstance() = INSTANCE
    }

    override fun <T : Any?> newInstantiatorOf(type: Class<T>): ObjectInstantiator<T> {
        if (type.constructors.isEmpty() && type.fields.any { it.name=="INSTANCE" }) {
            return KotlinObjectInstantiator(type)
        } else {
            return fallback.newInstantiatorOf(type)
        }
    }
}