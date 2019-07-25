package nl.adaptivity.android.coroutinesCompat

import android.support.v7.app.AppCompatActivity
import kotlinx.coroutines.*
import nl.adaptivity.android.coroutines.ActivityCoroutineScopeWrapper
import nl.adaptivity.android.coroutines.AndroidContextCoroutineScope
import nl.adaptivity.android.coroutines.contexts.AndroidContext
import kotlin.coroutines.CoroutineContext

open class CompatCoroutineActivity<A: CompatCoroutineActivity<A>>: AppCompatActivity(),
    AndroidContextCoroutineScope<A, ActivityCoroutineScopeWrapper<A>> {

    override fun getAndroidContext(): A = this as A

    override val coroutineContext: CoroutineContext =
        Job() + Dispatchers.Default + AndroidContext(this)

    override fun createScopeWrapper(parentScope: CoroutineScope): ActivityCoroutineScopeWrapper<A> {
        return ActivityCoroutineScopeWrapper(parentScope)
    }

    override fun onDestroy() {
        coroutineContext.cancel(CancellationException("Activity is being destroyed"))
        super.onDestroy()
    }
}