package nl.adaptivity.android.coroutines

import android.app.Fragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlin.coroutines.CoroutineContext

open class CoroutineFragment<F: CoroutineFragment<F>>: Fragment(),
    FragmentCoroutineScope<F> {
    override val coroutineContext: CoroutineContext = Job() + Dispatchers.Default

    @Suppress("UNCHECKED_CAST")
    override val fragment: F get() = this as F

    override fun onDestroy() {
        super.onDestroy()
        coroutineContext.cancel()
    }
}