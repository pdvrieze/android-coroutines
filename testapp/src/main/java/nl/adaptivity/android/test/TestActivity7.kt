package nl.adaptivity.android.test

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_test6.*
import kotlinx.android.synthetic.main.fragment_test6.view.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import nl.adaptivity.android.coroutines.CoroutineFragment
import nl.adaptivity.android.coroutines.aLaunch
import nl.adaptivity.android.coroutines.startActivityForResult

class TestFragment7: CoroutineFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_test6, container, false).also { root ->
            root.button.setOnClickListener { onButtonClick() }
        }
    }


    private fun onButtonClick() {
        Log.w(TestActivity7.TAG, "Activity is: $this")
        aLaunch(start = CoroutineStart.UNDISPATCHED, context = Dispatchers.Main) {
            val activityResult = startActivityForResult<TestActivity2>()

            val textView = findViewById<TextView>(R.id.textView)!!

            Log.w(TestActivity7.TAG, "Deserialised Activity is: ${activity()}")
            val newText = activityResult.flatMap { it?.getCharSequenceExtra(TestActivity2.KEY_DATA) } ?: getString(R.string.lbl_cancelled)
            Log.w(TestActivity7.TAG, "newText: $newText")

            Log.w(TestActivity7.TAG, "textview value: $textView")
            textView.text = newText
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState!=null) restoredView.text = getString(R.string.lbl_restored)
    }

}

/**
 * Implementation of an activity that uses an async launch to get a result from an activity using
 * coroutines. It uses the "safe" launch function and a synthetic accessor for the contained views.
 */
class TestActivity7 : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test7)
    }

    companion object {
        const val TAG="TestActivity7"
    }
}
