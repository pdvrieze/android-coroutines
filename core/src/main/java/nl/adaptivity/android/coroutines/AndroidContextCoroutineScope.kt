package nl.adaptivity.android.coroutines

import android.content.Context
import kotlinx.coroutines.*
import nl.adaptivity.android.coroutines.contexts.AndroidContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.launch as originalLaunch
import kotlinx.coroutines.async as originalAsync

/**
 * Interface for all sources of coroutine scope that can provide an android context
 */
interface AndroidContextCoroutineScope<out C : Context, out S : WrappedContextCoroutineScope<C, S>> :
    CoroutineScope {
    fun getAndroidContext(): C

    fun createScopeWrapper(parentScope: CoroutineScope): S

    fun launch(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend S.() -> Unit
    ): Job {
        val extContext = context + coroutineContext[AndroidContext]!!
        return originalLaunch(extContext, start) { createScopeWrapper(this).block() }
    }


    fun <R> async(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend S.() -> R
    ): Deferred<R> {
        val extContext = context + coroutineContext[AndroidContext]!!
        return originalAsync(
            extContext,
            start
        ) { createScopeWrapper(this).block() }
    }

}