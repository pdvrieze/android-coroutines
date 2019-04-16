@file:UseExperimental(ExperimentalTypeInference::class)

package nl.adaptivity.android.coroutines

import android.accounts.Account
import android.app.Activity
import android.app.Fragment
import android.app.FragmentManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.annotation.IdRes
import android.support.annotation.RequiresApi
import android.support.annotation.RequiresPermission
import android.view.View
import kotlinx.android.extensions.LayoutContainer
import kotlinx.coroutines.*
import java.io.Serializable
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.coroutineContext as extCoroutineContext
import kotlin.coroutines.suspendCoroutine
import kotlin.experimental.ExperimentalTypeInference
import kotlin.internal.*
import kotlinx.coroutines.launch as originalLaunch
import kotlinx.coroutines.async as originalAsync

/**
 * Version of the launch function for android usage. It provides convenience access to context objects
 * in a safer way. The scope interface can also be the receiver of further convenience extensions.
 *
 * The function works analogous to [launch].
 */
fun <A : CoroutineActivity> A.aLaunch(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend ActivityCoroutineScope<A,*>.() -> Unit
): Job {
    return originalLaunch(context + ActivityContext(this), start) {
        ActivityCoroutineScopeWrapper<A>(this).block()
    }
}

/*
@BuilderInference
public fun RetainingCoroutineScope.launchRetained(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job {
    val newContext = retainingContext + context
    return originalLaunch(newContext, start, block)
}
*/

fun Activity.ensureRetainingFragment(): RetainedContinuationFragment {
    val fm = fragmentManager
    val existingFragment =
        fm.findFragmentByTag(RetainedContinuationFragment.TAG) as RetainedContinuationFragment?

    if (existingFragment != null) return existingFragment

    val contFragment = RetainedContinuationFragment()
    fm.beginTransaction().apply {
        // This shouldn't happen, but in that case remove the old continuation.
        existingFragment?.let { remove(it) }

        add(contFragment, RetainedContinuationFragment.TAG)
    }.commit()
    runOnUiThread { fm.executePendingTransactions() }

    return contFragment
}

@Suppress("unused")
        /**
         * Version of the async function for android usage. It provides convenience access to context objects
         * in a safer way. The scope interface can also be the receiver of further convenience extensions.
         *
         * The function works analogous to [async].
         */
fun <A : CoroutineActivity, R> A.aAsync(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend ActivityCoroutineScope<A, *>.() -> R
): Deferred<R> {

    return originalAsync(context + ActivityContext(this), start) {
        ActivityCoroutineScopeWrapper<A>(
            this
        ).block()
    }
}

/**
 * Version of the launch function for android usage. It provides convenience access to context objects
 * in a safer way. The scope interface can also be the receiver of further convenience extensions.
 *
 * The function works analogous to [launch].
 */
@Suppress("unused")
suspend fun <A : CoroutineActivity, R> ActivityCoroutineScope<A, *>.aLaunch(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    parent: Job? = null,
    block: suspend ActivityCoroutineScope<A, *>.() -> R
): Job {
    return originalLaunch(context + activityContext(), start) {
        ActivityCoroutineScopeWrapper<A>(this@aLaunch).block()
    }
}

/**
 * Version of the async function for android usage. It provides convenience access to context objects
 * in a safer way. The scope interface can also be the receiver of further convenience extensions.
 *
 * The function works analogous to [async].
 */
@Suppress("unused")
suspend fun <A : CoroutineActivity, R> ActivityCoroutineScope<A, *>.aAsync(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend ActivityCoroutineScope<A, *>.() -> R
): Deferred<R> {

    return originalAsync (
        context + activityContext(),
        start
    ) { ActivityCoroutineScopeWrapper<A>(this).block() }
}

/**
 * Version of the launch function for android usage. It provides convenience access to context objects
 * in a safer way. The scope interface can also be the receiver of further convenience extensions.
 *
 * The function works analogous to [launch].
 */
fun <R> CoroutineScope.launch(
    applicationContext: Context,
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    parent: Job? = null,
    block: suspend ContextedCoroutineScope<Context, *>.() -> R
): Job {
    return originalLaunch(
        context + ApplicationContext(applicationContext),
        start
    ) { ApplicationCoroutineScopeWrapper(this).block() }
}

/**
 * Version of the async function for android usage. It provides convenience access to context objects
 * in a safer way. The scope interface can also be the receiver of further convenience extensions.
 *
 * The function works analogous to [async].
 */
fun <R> CoroutineScope.async(
    applicationContext: Context,
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    parent: Job? = null,
    block: suspend ContextedCoroutineScope<Context, *>.() -> R
): Deferred<R> {

    return originalAsync(
        context + ApplicationContext(applicationContext),
        start
    ) { ApplicationCoroutineScopeWrapper(this).block() }
}

/**
 * Version of the launch function for android usage. It provides convenience access to context objects
 * in a safer way. The scope interface can also be the receiver of further convenience extensions.
 *
 * The function works analogous to [launch].
 */
@Suppress("DEPRECATION", "unused")
fun <F : CoroutineFragment, R> F.aLaunch(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    parent: Job? = null,
    block: suspend FragmentCoroutineScope<F, Activity>.() -> R
): Job {
    // These are local vars as we want to resolve them immediately, not in the coroutine
    val tag = tag
    val id = id

    return originalLaunch(context + ActivityContext(activity), start) {
        createFragmentWrapper<F>(this, tag, id).block()
    }
}

private fun <F : Fragment> createFragmentWrapper(parent: CoroutineScope, tag: String?, id: Int) =
    when (tag) {
        null -> FragmentCoroutineScopeWrapper<F>(parent, id)
        else -> FragmentCoroutineScopeWrapper<F>(parent, tag)
    }


/**
 * Version of the async function for android usage. It provides convenience access to context objects
 * in a safer way. The scope interface can also be the receiver of further convenience extensions.
 *
 * The function works analogous to [async].
 */
@Suppress("DEPRECATION", "unused")
fun <F : CoroutineFragment, R> F.aAsync(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    parent: Job? = null,
    block: suspend FragmentCoroutineScope<F, Activity>.() -> R
): Deferred<R> {
    // These are local vars as we want to resolve them immediately, not in the coroutine
    val tag = tag
    val id = id

    return originalAsync(
        context + ActivityContext(activity),
        start
    ) { createFragmentWrapper<F>(this, tag, id).block() }
}

abstract class LayoutContainerScopeWrapper<A : Activity, out S : LayoutContainerCoroutineScope<A, S>>(
    private val parent: CoroutineScope
) : LayoutContainerCoroutineScope<A, S> {

    @Suppress("UNCHECKED_CAST")
    suspend fun activityContext(): ActivityContext<out A> =
        (extCoroutineContext[ActivityContext] as ActivityContext<out A>?)
            ?: throw IllegalStateException("Missing activity context")

    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "DEPRECATION")
    @Deprecated(
        "Replace with top-level coroutineContext",
        replaceWith = ReplaceWith(
            "coroutineContext",
            imports = ["kotlin.coroutines.experimental.coroutineContext"]
        )
    )
    @LowPriorityInOverloadResolution
    override val coroutineContext: CoroutineContext
        get() = parent.coroutineContext

    @Deprecated("Use layoutContainer or the function variant", ReplaceWith("layoutContainer()"))
    override val containerView: View?
        @Suppress("DEPRECATION")
        get() = activity.window.decorView

    override suspend fun containerView(): View? = activity().window.decorView

    abstract suspend fun <R> layoutContainer(body: LayoutContainer.() -> R): R

    @Deprecated("Use function", ReplaceWith("activity()"))
    @Suppress("UNCHECKED_CAST", "OverridingDeprecatedMember", "DEPRECATION")
    override val activity: A
        get() {
            // This is unsafe, but on creation the right activity should be set
            return coroutineContext[ActivityContext]?.activity as A
        }

    @Suppress("UNCHECKED_CAST")
    override suspend fun activity(): A = activityContext().activity

    /**
     * Asynchronously invoke [Activity.startActivityForResult] returning the result on completion.
     */
    override suspend fun startActivityForResult(intent: Intent): ActivityResult {
        return suspendCoroutine { continuation ->
            val activity = continuation.context[ActivityContext]?.activity ?: throw IllegalStateException("Missing activity in context")

            val contFragment: RetainedContinuationFragment = activity.ensureRetainingFragment()
            val resultCode: Int = contFragment.lastResultCode + 1

            contFragment.addContinuation(ParcelableContinuation(continuation, activity, resultCode))

            activity.runOnUiThread {
                contFragment.startActivityForResult(intent, resultCode)
            }
        }
    }

}

private class ApplicationCoroutineScopeWrapper(val parent: CoroutineScope) :
    ContextedCoroutineScope<Context, ApplicationCoroutineScopeWrapper> {

    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "DEPRECATION")
    @Deprecated(
        "Replace with top-level coroutineContext",
        replaceWith = ReplaceWith(
            "coroutineContext",
            imports = ["kotlin.coroutines.experimental.coroutineContext"]
        )
    )
    @LowPriorityInOverloadResolution
    override val coroutineContext: CoroutineContext
        get() = parent.coroutineContext

    @Suppress("UNCHECKED_CAST")
    suspend fun applicationContext(): ApplicationContext = extCoroutineContext[ApplicationContext]
        ?: throw IllegalStateException("Missing application context")

    override suspend fun getAndroidContext(): Context {
        return applicationContext().applicationContext
    }


    override suspend fun launch(
        context: CoroutineContext,
        start: CoroutineStart,
        block: suspend ApplicationCoroutineScopeWrapper.() -> R
    ): Job {
        return originalLaunch (
            context + applicationContext(),
            start
        ) { ApplicationCoroutineScopeWrapper(this).block() }
    }

    override suspend fun <R> async(
        context: CoroutineContext,
        start: CoroutineStart,
        block: suspend ApplicationCoroutineScopeWrapper.() -> R
    ): Deferred<R> {
        return originalAsync(
            context + applicationContext(),
            start
        ) { ApplicationCoroutineScopeWrapper(this).block() }
    }

}

private class DelegateLayoutContainer(override val containerView: View?) : LayoutContainer

private class ActivityCoroutineScopeWrapper<A : Activity>(parent: CoroutineScope) :
    LayoutContainerScopeWrapper<A, ActivityCoroutineScopeWrapper<A>>(parent),
    ActivityCoroutineScope<A, ActivityCoroutineScopeWrapper<A>> {

    override suspend fun launch(
        context: CoroutineContext,
        start: CoroutineStart,
        block: suspend ActivityCoroutineScopeWrapper<A>.() -> R
    ): Job {
        return originalLaunch(
            context + activityContext(),
            start
        ) { ActivityCoroutineScopeWrapper<A>(this).block() }
    }

    override suspend fun <R> async(
        context: CoroutineContext,
        start: CoroutineStart,
        block: suspend ActivityCoroutineScopeWrapper<A>.() -> R
    ): Deferred<R> {
        return originalAsync(
            context + activityContext(),
            start
        ) { ActivityCoroutineScopeWrapper<A>(this).block() }
    }

    @Suppress("unused")
    override suspend fun <R> layoutContainer(body: LayoutContainer.() -> R): R {
        return DelegateLayoutContainer(activity().window.decorView).body()
    }

    @Suppress("unused")
    suspend inline fun withActivity(body: A.() -> R): R {
        return activity().body()
    }

}

@Suppress("DEPRECATION")
private class FragmentCoroutineScopeWrapper<out F : Fragment>
private constructor(parent: CoroutineScope, private val tag: String?, private val id: Int) :
    LayoutContainerScopeWrapper<Activity, FragmentCoroutineScope<F, Activity>>(parent),
    FragmentCoroutineScope<F, Activity> {

    constructor(parent: CoroutineScope, tag: String) : this(parent, tag, 0)

    constructor(parent: CoroutineScope, id: Int) : this(parent, null, id)

    @Suppress("UNCHECKED_CAST", "OverridingDeprecatedMember")
    override val fragment: F
        get() = activity.fragmentManager.run {
            tag?.let { findFragmentByTag(it) } ?: findFragmentById(id)
        } as F

    @Suppress("UNCHECKED_CAST")
    override suspend fun fragment(): F {
        return activityContext().activity.fragmentManager.run {
            tag?.let { findFragmentByTag(it) } ?: findFragmentById(id)
        } as F
    }

    @Suppress("OverridingDeprecatedMember")
    override val fragmentManager: FragmentManager
        get() = fragment.fragmentManager

    @Suppress("DEPRECATION")
    override suspend fun fragmentManager(): FragmentManager {
        return activityContext().activity.fragmentManager
    }

    @Suppress("unused")
    override suspend fun <R> layoutContainer(body: LayoutContainer.() -> R): R {
        return DelegateLayoutContainer(fragment().view).body()
    }

    internal fun createScopeWrapper(coroutineScope: CoroutineScope) = when (tag) {
        null -> FragmentCoroutineScopeWrapper<F>(coroutineScope, id)
        else -> FragmentCoroutineScopeWrapper<F>(coroutineScope, tag)
    }


    override suspend fun launch(
        context: CoroutineContext,
        start: CoroutineStart,
        block: suspend FragmentCoroutineScope<F, Activity>.() -> R
    ): Job {
        return originalLaunch(
            context + activityContext(),
            start
        ) { createScopeWrapper(this).block() }
    }

    override suspend fun <R> async(
        context: CoroutineContext,
        start: CoroutineStart,
        block: suspend FragmentCoroutineScope<F, Activity>.() -> R
    ): Deferred<R> {
        return originalAsync(
            context + activityContext(),
            start
        ) { createScopeWrapper(this).block() }
    }
}


class ActivityContext<A : Activity>(activity: A) :
    AbstractCoroutineContextElement(ActivityContext) {
    var activity = activity
        internal set

    companion object Key : CoroutineContext.Key<ActivityContext<*>>, Serializable

    override fun toString(): String = "ActivityContext"

}

class ApplicationContext(applicationContext: Context) :
    AbstractCoroutineContextElement(ApplicationContext) {
    var applicationContext: Context = applicationContext.applicationContext
        internal set

    companion object Key : CoroutineContext.Key<ApplicationContext>

    override fun toString(): String = "ApplicationContext"
}

interface ContextedCoroutineScope<out C : Context, out S : ContextedCoroutineScope<C, S>> :
    CoroutineScope {
    suspend fun getAndroidContext(): C

    suspend fun Account.hasFeatures(features: Array<String?>) =
        accountHasFeaturesImpl(this, features)

    suspend fun launch(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend S.() -> R
    ): Job


    suspend fun <R> async(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend S.() -> R
    ): Deferred<R>

}

interface LayoutContainerCoroutineScope<out A : Activity, out S : LayoutContainerCoroutineScope<A, S>> :
    ContextedCoroutineScope<A, S>, LayoutContainer {
    @Deprecated("Use function", ReplaceWith("activity()"))
    val activity: A

    @Suppress("DEPRECATION")
    @Deprecated("Use function", ReplaceWith("fragmentManager()"))
    val fragmentManager: FragmentManager

    suspend fun activity(): A

    @Suppress("DEPRECATION")
    suspend fun fragmentManager(): FragmentManager

    override suspend fun getAndroidContext() = activity()

    suspend fun containerView(): View?

    suspend fun <T : View> findViewById(@IdRes id: Int): T? = containerView()?.findViewById(id)

    @RequiresPermission("android.permission.USE_CREDENTIALS")
    suspend fun Account.getAuthToken(authTokenType: String, options: Bundle? = null) =
        getAuthToken(this, authTokenType, options)

    suspend fun startActivityForResult(intent: Intent): ActivityResult

    suspend fun startActivity(intent: Intent) = activity().startActivity(intent)

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    suspend fun startActivity(intent: Intent, options: Bundle) =
        activity().startActivity(intent, options)
}

@Suppress("DEPRECATION")
interface FragmentCoroutineScope<out F : Fragment, A : Activity> :
    LayoutContainerCoroutineScope<A, FragmentCoroutineScope<F, A>> {
    @Deprecated("Use function version")
    val fragment: F

    suspend fun fragment(): F
}

interface ActivityCoroutineScope<out A : Activity, out S : ActivityCoroutineScope<A, S>> :
    LayoutContainerCoroutineScope<A, S> {

    @Suppress("DEPRECATION", "OverridingDeprecatedMember")
    override val fragmentManager: FragmentManager
        get() = activity.fragmentManager

    @Suppress("DEPRECATION")
    override suspend fun fragmentManager(): FragmentManager {
        return activityContext().activity.fragmentManager
    }

    suspend fun activityContext(): ActivityContext<out A>
}

@Suppress("unused")
suspend inline fun <reified A> FragmentCoroutineScope<*, *>.startActivityForResult() =
    startActivityForResult(Intent(activity(), A::class.java))

suspend inline fun <reified A> ActivityCoroutineScope<*, *>.startActivityForResult() =
    startActivityForResult(Intent(activity(), A::class.java))

inline fun <reified A> Activity.startActivityForResult(requestCode: Int) =
    this.startActivityForResult(Intent(this, A::class.java), requestCode)

@Suppress("unused", "DEPRECATION")
inline fun <reified A> Fragment.startActivityForResult(requestCode: Int) =
    this.startActivityForResult(Intent(activity, A::class.java), requestCode)

@Suppress("unused")
inline fun <reified A> Activity.startActivity() = startActivity(Intent(this, A::class.java))

@Suppress("unused", "DEPRECATION")
inline fun <reified A> Fragment.startActivity() = startActivity(Intent(activity, A::class.java))

