package nl.adaptivity.android.test

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_test1.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import nl.adaptivity.android.coroutines.CoroutineActivity
import nl.adaptivity.android.coroutines.aLaunch
import nl.adaptivity.android.coroutines.startActivityForResult

/**
 * Implementation of an activity that uses an async launch to get a result from an activity using
 * coroutines. It uses the "safe" launch function and a synthetic accessor for the contained views.
 */
class TestActivity4 : CoroutineActivity() {

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        restoredView.text = getString(R.string.lbl_restored)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test1)
        button.setOnClickListener { onButtonClick() }
    }

    fun onButtonClick() {
        Log.w(TAG, "Activity is: $this")
        aLaunch(start = CoroutineStart.UNDISPATCHED, context = Dispatchers.Main) {
            val activityResult = startActivityForResult<TestActivity2>()

            Log.w(TAG, "Deserialised Activity is: $activity")
            val newText = activityResult.flatMap { it?.getCharSequenceExtra(TestActivity2.KEY_DATA) } ?: getString(R.string.lbl_cancelled)
            Log.w(TAG, "newText: $newText")

            Log.w(TAG, "textview value: $textView")
            textView.text = newText
        }
    }

    companion object {
        const val TAG="TestActivity4"
    }
}
