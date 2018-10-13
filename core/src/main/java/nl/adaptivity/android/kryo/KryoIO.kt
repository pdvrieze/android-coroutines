@file:Suppress("unused")

package nl.adaptivity.android.kryo

import android.content.Context
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.util.MapReferenceResolver
import kotlinx.coroutines.CommonPool
import nl.adaptivity.android.kryo.serializers.ObjectSerializer
import nl.adaptivity.android.kryo.serializers.SafeContinuationSerializer
import nl.adaptivity.android.kryo.serializers._SafeContinuation
import org.objenesis.strategy.StdInstantiatorStrategy


/**
 * Get a Kryo serializer for a context-less application. For serialization this should not make
 * a difference, but for deserialization any contexts present in the state will lead to failure.
 */
val kryoAndroid get(): Kryo = Kryo(AndroidKotlinResolver(null), MapReferenceResolver()).apply { registerAndroidSerializers() }

/**
 * Get a Kryo serializer that handles Android contexts special. It allows dynamic replacement of
 * markers indicating a context with the passed in context (or application context if that applies).
 */
fun kryoAndroid(context: Context?): Kryo = Kryo(AndroidKotlinResolver(context), MapReferenceResolver()).apply { registerAndroidSerializers() }

/**
 * Extension function
 */
fun Kryo.registerAndroidSerializers() {
    instantiatorStrategy = KotlinObjectInstantiatorStrategy(Kryo.DefaultInstantiatorStrategy(StdInstantiatorStrategy()))

    register(_SafeContinuation, SafeContinuationSerializer(this))
    /* TODO While this doesn't affect instantiation (The KotlinObjectStantiatorStrategy handles that)
     * this may be needed to not serialize/deserialize the actual pool state.
     */
    register(CommonPool::class.java, ObjectSerializer(this, CommonPool::class.java))
}

