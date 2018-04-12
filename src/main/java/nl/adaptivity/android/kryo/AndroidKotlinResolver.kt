package nl.adaptivity.android.kryo

import android.accounts.AccountManager
import android.app.Activity
import android.app.Fragment
import android.content.Context
import com.esotericsoftware.kryo.Registration
import com.esotericsoftware.kryo.serializers.FieldSerializer
import com.esotericsoftware.kryo.util.DefaultClassResolver
import kotlinx.coroutines.experimental.android.HandlerContext
import kotlinx.coroutines.experimental.android.UI
import nl.adaptivity.android.kryo.serializers.*
import java.lang.ref.Reference

open class AndroidKotlinResolver(protected val context: Context?) : DefaultClassResolver() {

    override fun getRegistration(type: Class<*>): Registration? {
        val c = context
        val superReg = super.getRegistration(type)
        return when {
            superReg!=null -> superReg
            type.superclass==null -> superReg
            HandlerContext::class.java.isAssignableFrom(type) ->
                register(Registration(type, kryo.pseudoObjectSerializer(UI), NAME))
            c!=null && c.javaClass == type ->
                register(Registration(type, ContextSerializer(context), NAME))
            Context::class.java.isAssignableFrom(type.superclass) ->
                register(Registration(type, ContextSerializer(context), NAME))
            context is Activity && Fragment::class.java.isAssignableFrom(type.superclass) ->
                register(Registration(type, FragmentSerializer(context), NAME))
            Reference::class.java.isAssignableFrom(type) ->
                register(Registration(type, ReferenceSerializer(kryo, type.asSubclass(Reference::class.java)), NAME))
            Function::class.java.isAssignableFrom(type.superclass) ->
                register(Registration(type, FieldSerializer<Any>(kryo, type).apply { setIgnoreSyntheticFields(false) }, NAME))
            AccountManager::class.java.isAssignableFrom(type) ->
                register(Registration(type, AccountManagerSerializer(kryo, type, c), NAME))
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