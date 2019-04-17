package nl.adaptivity.android.test


import android.content.Intent
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.Espresso.pressBack
import android.support.test.espresso.action.ViewActions.*
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.view.View
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import junit.framework.Assert.assertEquals
import nl.adaptivity.android.kryo.LineOutput
import nl.adaptivity.android.kryo.kryoAndroid
import com.esotericsoftware.minlog.Log as KryoLog
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

@LargeTest
@RunWith(AndroidJUnit4::class)
class TestActivity1Test {

    @Before
    fun setLogging() {
        KryoLog.set(KryoLog.LEVEL_TRACE)
    }

    @Rule
    @JvmField
    val activity1TestRule = ActivityTestRule(TestActivity1::class.java, false, false)

    @Rule
    @JvmField
    val activity3TestRule = ActivityTestRule(TestActivity3::class.java, false, false)

    @Rule
    @JvmField
    val activity4TestRule = ActivityTestRule(TestActivity4::class.java, false, false)

    @Rule
    @JvmField
    val activity5TestRule = ActivityTestRule(TestActivity5::class.java, false, false)

    @Rule
    @JvmField
    val activity6TestRule = ActivityTestRule(TestActivity6::class.java, false, false)

    @Rule
    @JvmField
    val activity7TestRule = ActivityTestRule(TestActivity7::class.java, false, false)

    @Test
    @Throws(Throwable::class)
    fun testActivity1Test1() {
        textActivity(activity1TestRule)

    }

    @Test
    @Throws(Throwable::class)
    fun testActivity3Test1() {
        textActivity(activity3TestRule)
    }

    @Test
    @Throws(Throwable::class)
    fun testActivity4Test1() {
        textActivity(activity4TestRule)
    }

    @Test
    @Throws(Throwable::class)
    fun testActivity5Test1() {
        textActivity(activity5TestRule)
    }

    @Test
    @Throws(Throwable::class)
    fun testActivity6Test1() {
        textActivity(activity6TestRule)
    }

    @Test
    @Throws(Throwable::class)
    fun testActivity7Test1() {
        textActivity(activity7TestRule)
    }

    @Test
    fun testActivity7TestSerializeFragment() {
        activity7TestRule.launchActivity(null)
        activity7TestRule.runOnUiThread {
            val frag7 = activity7TestRule.activity.fragmentManager.findFragmentByTag("frag7outer")
            val baos = ByteArrayOutputStream()
            LineOutput(baos).use { lineOutput ->
                val kryo = kryoAndroid(activity7TestRule.activity)
                kryo.writeObject(lineOutput, frag7)
            }
            /*
             * 1 -> not null frag7
             * 1 -> not null FRAGMENTBYID
             * 8 -> FRAGMENTBYID enum ordinal
             * 1 -> writeName (not null?)
             * 0 -> nameId
             * ....... -> Name
             * 0x7f..... -> fragment id
             */
            val expected="1\n1\n8\n1\n0\nnl.adaptivity.android.test.TestFragment7\n${frag7.id}\n"
            assertEquals(expected, baos.toString("UTF8"))

            baos.reset()
            val kryo = kryoAndroid(activity7TestRule.activity)
            Output(baos).use { out ->
                kryo.writeObject(out, frag7)
            }
            val frag7cpy = kryo.readObject(Input(baos.toByteArray()), TestFragment7::class.java)
            assertEquals(frag7, frag7cpy)
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testActivity1Test2() {
        KryoLog.set(KryoLog.LEVEL_TRACE)

        activity1TestRule.launchActivity(null)
        run {
            // Activity 1
            launchActivity2()
        }
        activity1TestRule.runOnUiThread { activity1TestRule.activity.recreate() }

        run {
            // Activity 2
            activity2EnterText()

            pressBack()
        }

        run {
            checkRestored()

            val textView2 = onView(allOf<View>(withId(R.id.textView), isDisplayed()))
            textView2.check(matches(withText("Cancelled")))
        }

    }

    private fun textActivity(testRule: ActivityTestRule<*>) {
        testRule.launchActivity(null)
        run {
            // Activity 1
            launchActivity2()
        }

        testRule.runOnUiThread { testRule.activity.recreate() }

        run {
            // Activity 2
            activity2EnterText()

            val button = onView(allOf<View>(withId(R.id.button2), withText("Submit"), isDisplayed()))
            button.perform(click())

        }

        run {

            checkRestored()

            val textView2 = onView(allOf<View>(withId(R.id.textView), isDisplayed()))
            textView2.check(matches(withText("ghgh")))
        }
    }

    private fun checkRestored() {
        val restoredView = onView(withId(R.id.restoredView))
        restoredView.check(matches(allOf<View>(withText("Restored"), isDisplayed())))
    }

    private fun activity2EnterText() {
        val editText = onView(withId(R.id.textView2))
        editText.check(matches(allOf<View>(withHint("Provide some text here"), isDisplayed())))
        editText.perform(replaceText("ghgh"), closeSoftKeyboard())

        editText.check(matches(allOf<View>(withText("ghgh"), isDisplayed())))
    }

    private fun launchActivity2() {
        val textView = onView(withId(R.id.textView))
        textView.check(matches(allOf<View>(withText("TextView"), isDisplayed())))

        val restoredView = onView(withId(R.id.restoredView))
        restoredView.check(matches(withText("")))

        val button = onView(allOf<View>(withId(R.id.button), withText("Button"), isDisplayed()))
        button.perform(click())
    }

}
