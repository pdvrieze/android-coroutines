package nl.adaptivity.android.coroutines

import android.app.FragmentManager
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.view.View
import kotlinx.android.extensions.LayoutContainer
import kotlinx.coroutines.experimental.*
import kotlin.coroutines.experimental.CoroutineContext
import android.support.v4.app.Fragment as SupportFragment
import android.support.v4.app.FragmentManager as SupportFragmentManager


@Suppress("unused")
        /**
         * Verion of the launch function for android usage. It provides convenience access to context objects
         * in a safer way. The scope interface can also be the receiver of further convenience extensions.
         *
         * The function works analogous to [launch].
         */
fun <A : AppCompatActivity, R> A.aLaunch(context: CoroutineContext, start: CoroutineStart, parent: Job? = null, block: suspend ActivityCoroutineScope<A, *>.() -> R): Job {
    return launch(context + ActivityContext(this), start, parent) { AppcompatActivityCoroutineScopeWrapper<A>(this).block() }
}

@Suppress("unused")
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

private fun <F : SupportFragment> createFragmentWrapper(parent: CoroutineScope, tag: String?, id: Int): SupportFragmentCoroutineScopeWrapper<F> {
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
fun <F : SupportFragment, R> F.aLaunch(context: CoroutineContext, start: CoroutineStart, parent: Job? = null, block: suspend SupportFragmentCoroutineScope<F, AppCompatActivity>.() -> R): Job {
    val id = id
    val tag = tag
    return launch(context + ActivityContext(requireActivity()), start, parent) {
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
fun <F : SupportFragment, R> F.aAsync(context: CoroutineContext = DefaultDispatcher,
                                      start: CoroutineStart = CoroutineStart.DEFAULT,
                                      parent: Job? = null,
                                      block: suspend SupportFragmentCoroutineScope<F, AppCompatActivity>.() -> R): Deferred<R> {
    val id = id
    val tag = tag

    return async(context + ActivityContext(requireActivity()), start, parent) { createFragmentWrapper<F>(this, tag, id).block() }
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

    override suspend fun launch(context: CoroutineContext, start: CoroutineStart, parent: Job?, block: suspend AppcompatCoroutineScope<A>.() -> R): Job {
        return kotlinx.coroutines.experimental.launch(context + activityContext(), start, parent) {
            AppcompatActivityCoroutineScopeWrapper<A>(this).block()
        }
    }

    override suspend fun <R> async(context: CoroutineContext, start: CoroutineStart, parent: Job?, block: suspend AppcompatCoroutineScope<A>.() -> R): Deferred<R> {
        return kotlinx.coroutines.experimental.async(context + activityContext(), start, parent) {
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

    override suspend fun launch(context: CoroutineContext, start: CoroutineStart, parent: Job?, block: suspend SupportFragmentCoroutineScope<F, AppCompatActivity>.() -> R): Job {
        return kotlinx.coroutines.experimental.launch(context + activityContext(), start, parent) {
            createScopeWrapper(this).block()
        }
    }

    internal fun createScopeWrapper(coroutineScope: CoroutineScope) = when (tag) {
                null -> SupportFragmentCoroutineScopeWrapper<F>(coroutineScope, id)
                else -> SupportFragmentCoroutineScopeWrapper<F>(coroutineScope, tag)
            }

    override suspend fun <R> async(context: CoroutineContext, start: CoroutineStart, parent: Job?, block: suspend SupportFragmentCoroutineScope<F, AppCompatActivity>.() -> R): Deferred<R> {
        return kotlinx.coroutines.experimental.async(context + activityContext(), start, parent) {
            createScopeWrapper(this).block()
        }
    }


}

suspend inline fun <reified A> SupportFragmentCoroutineScope<*, *>.startActivityForResult() = startActivityForResult(Intent(activity(), A::class.java))


@Suppress("unused", "DEPRECATION")
inline fun <reified A> SupportFragment.startActivityForResult(requestCode: Int) = this.startActivityForResult(Intent(activity, A::class.java), requestCode)


@Suppress("unused", "DEPRECATION")
inline fun <reified A> SupportFragment.startActivity() = startActivity(Intent(activity, A::class.java))


