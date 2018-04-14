package nl.adaptivity.android.test

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_test1.*
import nl.adaptivity.android.coroutines.Maybe
import nl.adaptivity.android.coroutines.withActivityResult

/**
 * Version of the test activity that uses a callback rather than a coroutine.
 */
@SuppressLint("RestrictedApi")
class TestActivity1 : Activity() {

    private val resultHandler2: TestActivity1.(Maybe<Intent?>) -> Unit = { result ->
        result.onOk { data -> textView.text = data?.getCharSequenceExtra(TestActivity2.KEY_DATA)}
        result.onCancelled { textView.text = getString(R.string.lbl_cancelled) }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        restoredView.text = getString(R.string.lbl_restored)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test1)
        button.setOnClickListener({ _ ->

            withActivityResult(Intent(this@TestActivity1, TestActivity2::class.java), resultHandler2)
        })
    }
}
