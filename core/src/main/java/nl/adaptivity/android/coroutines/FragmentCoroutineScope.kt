package nl.adaptivity.android.coroutines

import android.app.Activity
import android.app.Fragment
import android.view.View
import kotlinx.android.extensions.LayoutContainer
import kotlinx.coroutines.*
import nl.adaptivity.android.coroutines.contexts.FragmentContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.launch as originalLaunch
import kotlinx.coroutines.async as originalAsync

/**
 * Interface for all sources of coroutine scope for an android fragment
 */
interface FragmentCoroutineScope<out F : Fragment> :
    CoroutineScope {

    val fragment: F

    fun getAndroidContext(): Activity? = fragment.activity

    fun createScopeWrapper(parentScope: CoroutineScope): FragmentCoroutineScopeWrapper<F> =
        FragmentCoroutineScopeWrapper(parentScope)

    fun launch(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend FragmentCoroutineScopeWrapper<F>.() -> Unit
    ): Job {
        val extContext = context + coroutineContext[FragmentContext]!!
        return originalLaunch(extContext, start) { createScopeWrapper(this).block() }
    }


    fun <R> async(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend FragmentCoroutineScopeWrapper<F>.() -> R
    ): Deferred<R> {
        val extContext = context + coroutineContext[FragmentContext]!!
        return originalAsync(extContext, start) { createScopeWrapper(this).block() }
    }

}