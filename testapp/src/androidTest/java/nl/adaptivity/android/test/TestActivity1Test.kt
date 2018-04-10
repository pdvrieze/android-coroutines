package nl.adaptivity.android.test


import android.content.Intent
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.Espresso.pressBack
import android.support.test.espresso.action.ViewActions.*
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.test.suitebuilder.annotation.LargeTest
import android.view.View
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class TestActivity1Test {

    @Rule
    @JvmField
    val activity1TestRule = ActivityTestRule(TestActivity1::class.java, false, false)

    @Rule
    @JvmField
    val activity3TestRule = ActivityTestRule(TestActivity3::class.java, false, false)

    @Test
    @Throws(Throwable::class)
    fun testActivity1Test1() {
        activity1TestRule.launchActivity(Intent())
        run {
            // Activity 1
            launchActivity2()
        }

        activity1TestRule.runOnUiThread { activity1TestRule.activity.recreate() }

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

    @Test
    @Throws(Throwable::class)
    fun testActivity3Test1() {
        com.esotericsoftware.minlog.Log.set(com.esotericsoftware.minlog.Log.LEVEL_DEBUG)
        activity3TestRule.launchActivity(Intent())
        run {
            // in Activity 3
            launchActivity2()
        }

        activity3TestRule.runOnUiThread { activity3TestRule.activity.recreate() }

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

    @Test
    @Throws(Throwable::class)
    fun testActivity1Test2() {
        activity1TestRule.launchActivity(Intent())
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
