package nl.adaptivity.android.kryo.serializers

import android.accounts.AccountManager
import android.content.Context
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output

/**
 * Serializer for Kotlin objects that stores nothing and just retrieves the current instance from
 * the field.
 */
internal class ObjectSerializer(kryo: Kryo, val type: Class<*>): Serializer<Any>(false, true) {
    /**
     * The correct way of getting an object is getting it's instance.
     */
    override fun read(kryo: Kryo, input: Input, type: Class<Any>): Any {
        return type.fields.first { it.name=="INSTANCE" }.get(null)
    }

    override fun write(kryo: Kryo, output: Output, obj: Any?) {
        // The class is already written by the caller so no need to write anything
    }
}

/**
 * Serializer for Kotlin objects that stores nothing and just retrieves the current instance from
 * the field.
 */
internal class AccountManagerSerializer(kryo: Kryo, val type: Class<*>, context: Context?): Serializer<Any>(false, true) {
    private val context = context?.applicationContext
    /**
     * The correct way of getting an object is getting it's instance.
     */
    override fun read(kryo: Kryo, input: Input, type: Class<Any>): Any {
        return AccountManager.get(context)
    }

    override fun write(kryo: Kryo, output: Output, obj: Any?) {
        // The class is already written by the caller so no need to write anything
    }
}
