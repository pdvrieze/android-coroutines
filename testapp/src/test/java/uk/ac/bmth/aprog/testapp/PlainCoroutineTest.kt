package uk.ac.bmth.aprog.testapp

import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import kotlinx.coroutines.*
import nl.adaptivity.android.kryo.kryoAndroid
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class PlainCoroutineTest {
    private suspend fun foo(): String {
        yield()
        return "2"
    }

    @Test
    fun testClosureImpl() {
        lateinit var coroutine: Continuation<Int>
        val deferred = GlobalScope.async(start = CoroutineStart.UNDISPATCHED) {
            val s = "Hello"
            val i1 = suspendCoroutine<Int> { cont ->
                coroutine = cont
            }
            assertEquals(2, i1)
            s
        }

        assertTrue(deferred.isActive)

        coroutine.resume(2)

//        assertTrue(deferred.isCompleted)

        val s2 = runBlocking { deferred.await() }
        assertEquals("Hello", s2)
    }

    @Test
    fun testClosureSerialization() {
//        val kryo = Kryo().apply { instantiatorStrategy = Kryo.DefaultInstantiatorStrategy(StdInstantiatorStrategy()) }
        val kryo = kryoAndroid

        lateinit var coroutine: Continuation<Int>

        // Create a coroutine and the suspend it.
        // It needs to run out of the blocking scope as it suspends. Otherwise it will never return.
        GlobalScope.async(start = CoroutineStart.UNDISPATCHED) {
            val s = "Hello"
            val i = suspendCoroutine<Int> { cont -> coroutine = cont }
            assertEquals(3, i)
            s
        }

        val baos = ByteArrayOutputStream()

        // Write the coroutine to a bytearray
        Output(baos).use { output ->
            kryo.writeClassAndObject(output, coroutine)
        }

        val serialized = baos.toByteArray()
        val deserializedCoroutine = kryo.readClassAndObject(Input(serialized)) as Continuation<Int>

        deserializedCoroutine.resume(3) // is not guaranteed to run here
        coroutine.resume(3)

        val deferredDeserialized = deserializedCoroutine.context[Job] as Deferred<String>

        val result = run {
            System.out.println("5")
            runBlocking {
                deferredDeserialized.await().apply {
                    System.out.println("6: ${this}")
                }
            }
        }
        System.out.println("7")

        assertEquals("Hello", result)

    }
}