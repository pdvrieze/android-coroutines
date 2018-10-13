package nl.adaptivity.android.test

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_test1.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.android.UI
import kotlinx.coroutines.launch
import nl.adaptivity.android.coroutines.activityResult

/**
 * Implementation of an activity that uses an async launch to get a result from an activity using
 * coroutines. It uses the standard launch function.
 */
//@SuppressLint("RestrictedApi")
class TestActivity3 : Activity() {

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
        launch(start = CoroutineStart.UNDISPATCHED, context = UI) {
            val activityResult = activityResult(Intent(this@TestActivity3, TestActivity2::class.java))
            Log.w(TAG, "Deserialised Activity is: ${this@TestActivity3}")
            val newText = activityResult.flatMap { it?.getCharSequenceExtra(TestActivity2.KEY_DATA) } ?: getString(R.string.lbl_cancelled)
            Log.w(TAG, "newText: $newText")
            val textView = findViewById<TextView>(R.id.textView)
            Log.w(TAG, "textview value: ${textView}")
            textView.text = newText
        }
    }

    companion object {
        const val TAG="TestActivity3"
    }
}
