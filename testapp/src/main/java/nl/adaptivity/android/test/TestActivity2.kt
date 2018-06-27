package nl.adaptivity.android.test

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_test2.*

/**
 * Simple activity that has a text box that can be passed as result.
 */
class TestActivity2 : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test2)
        button2.setOnClickListener { _ ->
            val result = Intent("result").apply { putExtra(KEY_DATA, textView2.text) }
            this@TestActivity2.setResult(Activity.RESULT_OK, result)
            finish()
        }
    }

    companion object {
        const val KEY_DATA="data"
    }
}
