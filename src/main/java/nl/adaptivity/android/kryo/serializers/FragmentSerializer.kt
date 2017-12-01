package nl.adaptivity.android.kryo.serializers

import android.app.Activity
import android.app.Fragment
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output

internal class FragmentSerializer(private val context: Activity?) : Serializer<Fragment>() {

    override fun read(kryo: Kryo, input: Input, type: Class<Fragment>): Fragment? {
        val marker = kryo.readObject(input, KryoAndroidConstants::class.java)
        val savedFragmentType:Class<*> = kryo.readClass(input).type
        val result: Fragment? = when (marker) {
            KryoAndroidConstants.FRAGMENTBYTAG ->
                context?.fragmentManager?.findFragmentByTag(input.readString())
            KryoAndroidConstants.FRAGMENTBYID ->
                context?.fragmentManager?.findFragmentById(input.readInt())
            else -> return null
        }

        if (! type.isAssignableFrom(savedFragmentType)) {
            throw ClassCastException("Saved a fragment of type ${savedFragmentType}, but asked to inflate as ${type}")
        }
        type.cast(result)

        return result?.also { kryo.reference(it) }
    }

    override fun write(kryo: Kryo, output: Output, obj: Fragment) {
        if (obj.id!=0) {
            kryo.writeObject(output, KryoAndroidConstants.FRAGMENTBYID)
            kryo.writeClass(output, obj.javaClass)
            output.writeInt(obj.id)
        } else {
            kryo.writeObject(output, KryoAndroidConstants.FRAGMENTBYTAG)
            kryo.writeClass(output, obj.javaClass)
            output.writeString(obj.tag)
        }
    }
}