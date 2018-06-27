package nl.adaptivity.android.kryo.serializers

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.util.Log
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output

internal class SupportFragmentSerializer(private val context: FragmentActivity?) : Serializer<Fragment>() {

    override fun read(kryo: Kryo, input: Input, type: Class<Fragment>): Fragment? {
        val marker = kryo.readObject(input, KryoAndroidConstants::class.java)
        val savedFragmentType: Class<*> = kryo.readClass(input).type
        val result: Fragment? = when (marker) {
            KryoAndroidConstants.FRAGMENTBYTAG -> {
                context?.supportFragmentManager?.findFragmentByTag(input.readString())
            }

            KryoAndroidConstants.FRAGMENTBYID ->
                context?.supportFragmentManager?.findFragmentById(input.readInt())

            KryoAndroidConstants.FRAGMENTWITHOUTHANDLE -> {
//                context?.fragmentManager?.fragments?.firstOrNull { savedFragmentType == it.javaClass }
                null
            }

            else -> return null
        }

        if (!type.isAssignableFrom(savedFragmentType)) {
            throw ClassCastException("Saved a fragment of type ${savedFragmentType}, but asked to inflate as ${type}")
        }
        Log.e("FragmentSerializer", "Deserialized fragment $result of type ${result?.javaClass}")
        type.cast(result)

        return result?.also { kryo.reference(it) }
    }

    override fun write(kryo: Kryo, output: Output, obj: Fragment) {
        if (obj.id != 0) {
            kryo.writeObject(output, KryoAndroidConstants.FRAGMENTBYID)
            kryo.writeClass(output, obj.javaClass)
            output.writeInt(obj.id)
        } else if (obj.tag != null) {
            kryo.writeObject(output, KryoAndroidConstants.FRAGMENTBYTAG)
            kryo.writeClass(output, obj.javaClass)
            output.writeString(obj.tag)
        } else {
            kryo.writeObject(output, KryoAndroidConstants.FRAGMENTWITHOUTHANDLE)
            kryo.writeClass(output, obj.javaClass)
        }
    }
}