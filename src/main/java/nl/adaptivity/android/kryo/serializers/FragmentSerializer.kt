package nl.adaptivity.android.kryo.serializers

import android.app.Activity
import android.app.Fragment
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output

internal class FragmentSerializer(private val context: Activity?) : Serializer<Fragment>() {

    override fun read(kryo: Kryo, input: Input, type: Class<Fragment>): Fragment? {
        val result: Fragment? = when (kryo.readObject(input, KryoAndroidConstants::class.java)) {
            KryoAndroidConstants.FRAGMENT -> {
                val savedFragmentType:Class<*> = kryo.readClass(input).type
                val fragmentTag = input.readString()
                val ac = context
                val frag = ac?.fragmentManager?.findFragmentByTag(fragmentTag)

                if (! type.isAssignableFrom(savedFragmentType)) {
                    throw ClassCastException("Saved a fragment of type ${savedFragmentType}, but asked to inflate as ${type}")
                }
                type.cast(frag)
            }
            KryoAndroidConstants.APPLICATIONCONTEXT -> type.cast(context?.applicationContext)
            else -> null
        }
        return result?.also { kryo.reference(it) }
    }

    override fun write(kryo: Kryo, output: Output, obj: Fragment) {
        kryo.writeObject(output, KryoAndroidConstants.FRAGMENT)
        kryo.writeClass(output, obj.javaClass)
        output.writeString(obj.tag)
    }
}