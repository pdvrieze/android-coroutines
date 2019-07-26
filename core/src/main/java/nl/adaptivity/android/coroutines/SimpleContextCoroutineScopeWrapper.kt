package nl.adaptivity.android.coroutines

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import nl.adaptivity.android.coroutines.contexts.AndroidContext

class SimpleContextCoroutineScopeWrapper<out C : Context>(
    parentScope: CoroutineScope
) :
    WrappedContextCoroutineScope<C, SimpleContextCoroutineScopeWrapper<C>>(parentScope) {

    override fun getAndroidContext() = coroutineContext[AndroidContext]!!.androidContext as C

    override fun createScopeWrapper(parentScope: CoroutineScope): SimpleContextCoroutineScopeWrapper<C> {
        return SimpleContextCoroutineScopeWrapper(parentScope)
    }

}