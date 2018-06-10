package nl.adaptivity.android.coroutines

import android.content.Context
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v7.app.AppCompatActivity
import kotlinx.coroutines.experimental.*
import kotlin.coroutines.experimental.CoroutineContext


/**
 * Verion of the launch function for android usage. It provides convenience access to context objects
 * in a safer way. The scope interface can also be the receiver of further convenience extensions.
 *
 * The function works analogous to [launch].
 */
fun <A : AppCompatActivity, R> A.aLaunch(context: CoroutineContext, start: CoroutineStart, parent: Job? = null, block: suspend ActivityCoroutineScope<A, *>.() -> R): Job {
    return launch(context + ActivityContext(this), start, parent) { AppcompatActivityCoroutineScopeWrapper<A>(this).block() }
}

/**
 * Verion of the async function for android usage. It provides convenience access to context objects
 * in a safer way. The scope interface can also be the receiver of further convenience extensions.
 *
 * The function works analogous to [async].
 */
fun <A : AppCompatActivity, R> A.aAsync(context: CoroutineContext = DefaultDispatcher,
                               start: CoroutineStart = CoroutineStart.DEFAULT,
                               parent: Job? = null,
                               block: suspend ActivityCoroutineScope<A, *>.() -> R): Deferred<R> {

    return async(context + ActivityContext(this), start, parent) { AppcompatActivityCoroutineScopeWrapper<A>(this).block() }
}

/**
 * Verion of the launch function for android usage. It provides convenience access to context objects
 * in a safer way. The scope interface can also be the receiver of further convenience extensions.
 *
 * The function works analogous to [launch].
 */
fun <F : Fragment, R> F.aLaunch(context: CoroutineContext, start: CoroutineStart, parent: Job? = null, block: suspend SupportFragmentCoroutineScope<F, AppCompatActivity>.() -> R): Job {
    return launch(context + ActivityContext(requireActivity()), start, parent) { SupportFragmentCoroutineScopeWrapper<F>(this, tag!!).block() }
}

/**
 * Verion of the async function for android usage. It provides convenience access to context objects
 * in a safer way. The scope interface can also be the receiver of further convenience extensions.
 *
 * The function works analogous to [async].
 */
fun <F : Fragment, R> F.aAsync(context: CoroutineContext = DefaultDispatcher,
                               start: CoroutineStart = CoroutineStart.DEFAULT,
                               parent: Job? = null,
                               block: suspend SupportFragmentCoroutineScope<F, AppCompatActivity>.() -> R): Deferred<R> {

    return async(context + ActivityContext(requireActivity()), start, parent) { SupportFragmentCoroutineScopeWrapper<F>(this, tag!!).block() }
}


interface AppcompatCoroutineScope<out A: AppCompatActivity>: ActivityCoroutineScope<A, AppcompatCoroutineScope<A>> {
    val supportFragmentManager: FragmentManager get() = activity.supportFragmentManager
}



interface SupportFragmentCoroutineScope<out F: Fragment, out A: AppCompatActivity>: LayoutContainerCoroutineScope<A, SupportFragmentCoroutineScope<F,A>> {
    val supportFragmentManager get() = fragment.fragmentManager
    val fragment: F
}

private class AppcompatActivityCoroutineScopeWrapper<out A : AppCompatActivity>(parent: CoroutineScope) :
        LayoutContainerScopeWrapper<A, AppcompatCoroutineScope<A>>(parent), AppcompatCoroutineScope<A> {

    override fun launch(context: CoroutineContext, start: CoroutineStart, parent: Job?, block: suspend AppcompatCoroutineScope<A>.() -> R): Job {
        return kotlinx.coroutines.experimental.launch(context + coroutineContext[ActivityContext]!!, start, parent) {
            AppcompatActivityCoroutineScopeWrapper<A>(this).block()
        }
    }

    override fun <R> async(context: CoroutineContext, start: CoroutineStart, parent: Job?, block: suspend AppcompatCoroutineScope<A>.() -> R): Deferred<R> {
        return kotlinx.coroutines.experimental.async(context + coroutineContext[ActivityContext]!!, start, parent) {
            AppcompatActivityCoroutineScopeWrapper<A>(this).block()
        }
    }
}

private class SupportFragmentCoroutineScopeWrapper<out F : Fragment>(parent: CoroutineScope, private val tag: String) : LayoutContainerScopeWrapper<AppCompatActivity, SupportFragmentCoroutineScope<F,AppCompatActivity>>(parent), SupportFragmentCoroutineScope<F, AppCompatActivity> {
    @Suppress("UNCHECKED_CAST")
    override val fragment: F
        get() = activity.supportFragmentManager.findFragmentByTag(tag) as F

    @Deprecated("Use the support fragment manager", ReplaceWith("supportFragmentManager"))
    override val fragmentManager: android.app.FragmentManager
        get() = activity.fragmentManager

    override fun launch(context: CoroutineContext, start: CoroutineStart, parent: Job?, block: suspend SupportFragmentCoroutineScope<F,AppCompatActivity>.() -> R): Job {
        return kotlinx.coroutines.experimental.launch(context + coroutineContext[ActivityContext]!!, start, parent) {
            SupportFragmentCoroutineScopeWrapper<F>(this, tag).block()
        }
    }

    override fun <R> async(context: CoroutineContext, start: CoroutineStart, parent: Job?, block: suspend SupportFragmentCoroutineScope<F,AppCompatActivity>.() -> R): Deferred<R> {
        return kotlinx.coroutines.experimental.async(context + coroutineContext[ActivityContext]!!, start, parent) {
            SupportFragmentCoroutineScopeWrapper<F>(this, tag).block()
        }
    }

}
