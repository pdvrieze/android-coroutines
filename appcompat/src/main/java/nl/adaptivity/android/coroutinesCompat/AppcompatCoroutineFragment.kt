package nl.adaptivity.android.coroutinesCompat

import android.support.v4.app.Fragment
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

open class AppcompatCoroutineFragment<F: AppcompatCoroutineFragment<F>>: Fragment(),
    AppcompatFragmentCoroutineScope<F> {
    override val coroutineContext: CoroutineContext = Job() + Dispatchers.Default + AppcompatFragmentContext(this)

    @Suppress("UNCHECKED_CAST")
    override val fragment: F get() = this as F

    override fun onDestroy() {
        coroutineContext.cancel(CancellationException("Fragment is being destroyed"))
        super.onDestroy()
    }
}