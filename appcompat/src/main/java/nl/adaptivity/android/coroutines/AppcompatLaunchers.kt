package nl.adaptivity.android.coroutines

import android.app.FragmentManager
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.view.View
import kotlinx.android.extensions.LayoutContainer
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import android.support.v4.app.Fragment as SupportFragment
import android.support.v4.app.FragmentManager as SupportFragmentManager
import kotlinx.coroutines.async as origAsync
import kotlinx.coroutines.launch as origLaunch


open class CompatCoroutineActivity: AppCompatActivity(), CoroutineScope {
    override val coroutineContext: CoroutineContext = Job()

    override fun onDestroy() {
        coroutineContext.cancel(CancellationException("Activity is being destroyed"))
        super.onDestroy()
    }
}

open class CompatCoroutineFragment: SupportFragment(), CoroutineScope {
    override val coroutineContext: CoroutineContext = Job()

    override fun onDestroy() {
        coroutineContext.cancel(CancellationException("Fragment is being destroyed"))
        super.onDestroy()
    }
}

@Suppress("unused")
        /**
         * Verion of the launch function for android usage. It provides convenience access to context objects
         * in a safer way. The scope interface can also be the receiver of further convenience extensions.
         *
         * The function works analogous to [launch].
         */
fun <A : CompatCoroutineActivity, R> A.aLaunch(context: CoroutineContext = EmptyCoroutineContext, start: CoroutineStart = CoroutineStart.DEFAULT, block: suspend ActivityCoroutineScope<A, *>.() -> R): Job {
    return origLaunch(context + ActivityContext(this), start) { AppcompatActivityCoroutineScopeWrapper<A>(this).block() }
}

@Suppress("unused")
        /**
         * Verion of the async function for android usage. It provides convenience access to context objects
         * in a safer way. The scope interface can also be the receiver of further convenience extensions.
         *
         * The function works analogous to [async].
         */
fun <A : CompatCoroutineActivity, R> A.aAsync(context: CoroutineContext = EmptyCoroutineContext,
                                        start: CoroutineStart = CoroutineStart.DEFAULT,
                                        block: suspend ActivityCoroutineScope<A, *>.() -> R): Deferred<R> {

    return origAsync(context + ActivityContext(this), start) { AppcompatActivityCoroutineScopeWrapper<A>(this).block() }
}

private fun <F : CompatCoroutineFragment> createFragmentWrapper(parent: CoroutineScope, tag: String?, id: Int): SupportFragmentCoroutineScopeWrapper<F> {
    val t = tag
    return when (t) {
        null -> SupportFragmentCoroutineScopeWrapper(parent, id)
        else -> SupportFragmentCoroutineScopeWrapper(parent, t)
    }
}

@Suppress("unused")
        /**
         * Verion of the launch function for android usage. It provides convenience access to context objects
         * in a safer way. The scope interface can also be the receiver of further convenience extensions.
         *
         * The function works analogous to [launch].
         */
fun <F : CompatCoroutineFragment, R> F.aLaunch(context: CoroutineContext = EmptyCoroutineContext, start: CoroutineStart = CoroutineStart.DEFAULT, block: suspend SupportFragmentCoroutineScope<F, AppCompatActivity>.() -> R): Job {
    val id = id
    val tag = tag
    return origLaunch(context + ActivityContext(requireActivity()), start) {
        createFragmentWrapper<F>(this, tag, id).block()
    }
}


@Suppress("unused")
        /**
         * Verion of the async function for android usage. It provides convenience access to context objects
         * in a safer way. The scope interface can also be the receiver of further convenience extensions.
         *
         * The function works analogous to [async].
         */
fun <F : CompatCoroutineFragment, R> F.aAsync(context: CoroutineContext = EmptyCoroutineContext,
                                      start: CoroutineStart = CoroutineStart.DEFAULT,
                                      parent: Job? = null,
                                      block: suspend SupportFragmentCoroutineScope<F, AppCompatActivity>.() -> R): Deferred<R> {
    val id = id
    val tag = tag

    return origAsync(context + ActivityContext(requireActivity()), start) { createFragmentWrapper<F>(this, tag, id).block() }
}


interface AppcompatCoroutineScope<out A : AppCompatActivity> : ActivityCoroutineScope<A, AppcompatCoroutineScope<A>> {
    @Suppress("DEPRECATION")
    @Deprecated("Use function", ReplaceWith("supportFragmentManager()"))
    val supportFragmentManager: SupportFragmentManager
        get() = activity.supportFragmentManager

    suspend fun supportFragmentManager(): SupportFragmentManager
}


interface SupportFragmentCoroutineScope<out F : SupportFragment, out A : AppCompatActivity> : LayoutContainerCoroutineScope<A, SupportFragmentCoroutineScope<F, A>> {
    @Suppress("DEPRECATION")
    @Deprecated("Use function", ReplaceWith("supportFragmentManager()"))
    val supportFragmentManager
        get() = fragment.fragmentManager

    @Deprecated("Use function", ReplaceWith("fragment()"))
    val fragment: F

    suspend fun supportFragmentManager(): SupportFragmentManager
    suspend fun fragment(): F
}

private class AppcompatActivityCoroutineScopeWrapper<A : AppCompatActivity>(parent: CoroutineScope) :
        LayoutContainerScopeWrapper<A, AppcompatCoroutineScope<A>>(parent), AppcompatCoroutineScope<A> {

    override suspend fun <R> layoutContainer(body: LayoutContainer.() -> R): R {
        return SupportDelegateLayoutContainer(activity().window.decorView).body()
    }

    override suspend fun supportFragmentManager(): android.support.v4.app.FragmentManager {
        return activityContext().activity.supportFragmentManager
    }

    override suspend fun launch(context: CoroutineContext, start: CoroutineStart, block: suspend AppcompatCoroutineScope<A>.() -> R): Job {
        return origLaunch(context + activityContext(), start) {
            AppcompatActivityCoroutineScopeWrapper<A>(this).block()
        }
    }

    override suspend fun <R> async(context: CoroutineContext, start: CoroutineStart, block: suspend AppcompatCoroutineScope<A>.() -> R): Deferred<R> {
        return origAsync(context + activityContext(), start) {
            AppcompatActivityCoroutineScopeWrapper<A>(this).block()
        }
    }
}

private class SupportDelegateLayoutContainer(override val containerView: View?) : LayoutContainer

private class SupportFragmentCoroutineScopeWrapper<out F : SupportFragment>
private constructor(parent: CoroutineScope, private val tag: String?, private val id: Int) :
        LayoutContainerScopeWrapper<AppCompatActivity, SupportFragmentCoroutineScope<F, AppCompatActivity>>(parent), SupportFragmentCoroutineScope<F, AppCompatActivity> {

    constructor(parent: CoroutineScope, tag: String) : this(parent, tag, 0)

    constructor(parent: CoroutineScope, id: Int) : this(parent, null, id)

    @Suppress("UNCHECKED_CAST", "OverridingDeprecatedMember", "DEPRECATION")
    override val fragment: F
        get() = activity.supportFragmentManager.run { tag?.let{ findFragmentByTag(it) } ?: findFragmentById(id) } as F

    @Suppress("DEPRECATION")
    @Deprecated("Use the support fragment manager", ReplaceWith("supportFragmentManager"))
    override val fragmentManager: android.app.FragmentManager
        get() = activity.fragmentManager


    @Suppress("DEPRECATION")
    override suspend fun fragmentManager(): FragmentManager {
        return activityContext().activity.fragmentManager
    }

    override suspend fun supportFragmentManager(): android.support.v4.app.FragmentManager {
        return activity().supportFragmentManager
    }

    override suspend fun fragment(): F {
        @Suppress("UNCHECKED_CAST")
        return supportFragmentManager().run { tag?.let{ findFragmentByTag(it) } ?: findFragmentById(id) } as F
    }

    override suspend fun <R> layoutContainer(body: LayoutContainer.() -> R): R {
        return SupportDelegateLayoutContainer(fragment().view).body()
    }

    override suspend fun launch(context: CoroutineContext, start: CoroutineStart, block: suspend SupportFragmentCoroutineScope<F, AppCompatActivity>.() -> R): Job {
        return origLaunch(context + activityContext(), start) {
            createScopeWrapper(this).block()
        }
    }

    internal fun createScopeWrapper(coroutineScope: CoroutineScope) = when (tag) {
                null -> SupportFragmentCoroutineScopeWrapper<F>(coroutineScope, id)
                else -> SupportFragmentCoroutineScopeWrapper<F>(coroutineScope, tag)
            }

    override suspend fun <R> async(context: CoroutineContext, start: CoroutineStart, block: suspend SupportFragmentCoroutineScope<F, AppCompatActivity>.() -> R): Deferred<R> {
        return origAsync(context + activityContext(), start) {
            createScopeWrapper(this).block()
        }
    }


}

suspend inline fun <reified A> SupportFragmentCoroutineScope<*, *>.startActivityForResult() = startActivityForResult(Intent(activity(), A::class.java))


@Suppress("unused", "DEPRECATION")
inline fun <reified A> SupportFragment.startActivityForResult(requestCode: Int) = this.startActivityForResult(Intent(activity, A::class.java), requestCode)


@Suppress("unused", "DEPRECATION")
inline fun <reified A> SupportFragment.startActivity() = startActivity(Intent(activity, A::class.java))


