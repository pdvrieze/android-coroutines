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
import kotlinx.coroutines.experimental.*
import java.io.Serializable
import kotlin.coroutines.experimental.AbstractCoroutineContextElement
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.coroutineContext as extCoroutineContext
import kotlin.coroutines.experimental.suspendCoroutine
import kotlin.internal.*

/**
 * Version of the launch function for android usage. It provides convenience access to context objects
 * in a safer way. The scope interface can also be the receiver of further convenience extensions.
 *
 * The function works analogous to [launch].
 */
fun <A : Activity, R> A.aLaunch(context: CoroutineContext = DefaultDispatcher,
                                start: CoroutineStart = CoroutineStart.DEFAULT,
                                parent: Job? = null,
                                block: suspend ActivityCoroutineScope<A, *>.() -> R): Job {
    return launch(context + ActivityContext(this), start, parent) {
        ActivityCoroutineScopeWrapper<A>(this).block()
    }
}

@Suppress("unused")
        /**
         * Version of the async function for android usage. It provides convenience access to context objects
         * in a safer way. The scope interface can also be the receiver of further convenience extensions.
         *
         * The function works analogous to [async].
         */
fun <A : Activity, R> A.aAsync(context: CoroutineContext = DefaultDispatcher,
                               start: CoroutineStart = CoroutineStart.DEFAULT,
                               parent: Job? = null,
                               block: suspend ActivityCoroutineScope<A, *>.() -> R): Deferred<R> {

    return async(context + ActivityContext(this), start, parent) { ActivityCoroutineScopeWrapper<A>(this).block() }
}

/**
 * Version of the launch function for android usage. It provides convenience access to context objects
 * in a safer way. The scope interface can also be the receiver of further convenience extensions.
 *
 * The function works analogous to [launch].
 */
@Suppress("unused")
suspend fun <A : Activity, R> ActivityCoroutineScope<A, *>.aLaunch(context: CoroutineContext = DefaultDispatcher,
                                                                   start: CoroutineStart = CoroutineStart.DEFAULT,
                                                                   parent: Job? = null,
                                                                   block: suspend ActivityCoroutineScope<A, *>.() -> R): Job {
    return kotlinx.coroutines.experimental.launch(context + activityContext(), start, parent) {
        ActivityCoroutineScopeWrapper<A>(this@launch).block()
    }
}

/**
 * Version of the async function for android usage. It provides convenience access to context objects
 * in a safer way. The scope interface can also be the receiver of further convenience extensions.
 *
 * The function works analogous to [async].
 */
@Suppress("unused")
suspend fun <A : Activity, R> ActivityCoroutineScope<A, *>.aAsync(context: CoroutineContext = DefaultDispatcher,
                                                                  start: CoroutineStart = CoroutineStart.DEFAULT,
                                                                  parent: Job? = null,
                                                                  block: suspend ActivityCoroutineScope<A, *>.() -> R): Deferred<R> {

    return kotlinx.coroutines.experimental.async(context + activityContext(), start, parent) { ActivityCoroutineScopeWrapper<A>(this).block() }
}

/**
 * Version of the launch function for android usage. It provides convenience access to context objects
 * in a safer way. The scope interface can also be the receiver of further convenience extensions.
 *
 * The function works analogous to [launch].
 */
fun <R> launch(applicationContext: Context,
               context: CoroutineContext = DefaultDispatcher,
               start: CoroutineStart = CoroutineStart.DEFAULT,
               parent: Job? = null,
               block: suspend ContextedCoroutineScope<Context, *>.() -> R): Job {
    return launch(context + ApplicationContext(applicationContext), start, parent) { ApplicationCoroutineScopeWrapper(this).block() }
}

/**
 * Version of the async function for android usage. It provides convenience access to context objects
 * in a safer way. The scope interface can also be the receiver of further convenience extensions.
 *
 * The function works analogous to [async].
 */
fun <R> async(applicationContext: Context,
              context: CoroutineContext = DefaultDispatcher,
              start: CoroutineStart = CoroutineStart.DEFAULT,
              parent: Job? = null,
              block: suspend ContextedCoroutineScope<Context, *>.() -> R): Deferred<R> {

    return async(context + ApplicationContext(applicationContext), start, parent) { ApplicationCoroutineScopeWrapper(this).block() }
}

/**
 * Version of the launch function for android usage. It provides convenience access to context objects
 * in a safer way. The scope interface can also be the receiver of further convenience extensions.
 *
 * The function works analogous to [launch].
 */
@Suppress("DEPRECATION", "unused")
fun <F : Fragment, R> F.aLaunch(context: CoroutineContext = DefaultDispatcher,
                                start: CoroutineStart = CoroutineStart.DEFAULT,
                                parent: Job? = null,
                                block: suspend FragmentCoroutineScope<F, Activity>.() -> R): Job {
    // These are local vars as we want to resolve them immediately, not in the coroutine
    val tag = tag
    val id = id

    return launch(context + ActivityContext(activity), start, parent) {
        createFragmentWrapper<F>(this, tag, id).block()
    }
}

private fun <F : Fragment> createFragmentWrapper(parent: CoroutineScope, tag: String?, id:Int) =
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
fun <F : Fragment, R> F.aAsync(context: CoroutineContext = DefaultDispatcher,
                               start: CoroutineStart = CoroutineStart.DEFAULT,
                               parent: Job? = null,
                               block: suspend FragmentCoroutineScope<F, Activity>.() -> R): Deferred<R> {
    // These are local vars as we want to resolve them immediately, not in the coroutine
    val tag = tag
    val id = id

    return async(context + ActivityContext(activity), start, parent) { createFragmentWrapper<F>(this, tag, id).block() }
}

abstract class LayoutContainerScopeWrapper<A : Activity, out S : LayoutContainerCoroutineScope<A, S>>(private val parent: CoroutineScope) : LayoutContainerCoroutineScope<A, S> {

    @Suppress("UNCHECKED_CAST")
    suspend fun activityContext(): ActivityContext<out A> = (extCoroutineContext[ActivityContext] as ActivityContext<out A>?)
            ?: throw IllegalStateException("Missing activity context")

    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "DEPRECATION")
    @Deprecated("Replace with top-level coroutineContext",
            replaceWith = ReplaceWith("coroutineContext",
                    imports = ["kotlin.coroutines.experimental.coroutineContext"]))
    @LowPriorityInOverloadResolution
    override val coroutineContext: CoroutineContext
        get() = parent.coroutineContext
    override val isActive: Boolean get() = parent.isActive

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
            val activity = continuation.context[ActivityContext]!!.activity
            @Suppress("DEPRECATION")
            val fm = activity.fragmentManager
            val existingFragment = fm.findFragmentByTag(RetainedContinuationFragment.TAG) as RetainedContinuationFragment?
            val contFragment: RetainedContinuationFragment = existingFragment?: RetainedContinuationFragment()
            val resultCode: Int = contFragment.lastResultCode+1

            contFragment.addContinuation(ParcelableContinuation(continuation, activity, resultCode))

            if (existingFragment == null) {
                fm.beginTransaction().apply {
                    // This shouldn't happen, but in that case remove the old continuation.
                    existingFragment?.let { remove(it) }

                    add(contFragment, RetainedContinuationFragment.TAG)
                }.commit()
            }

            activity.runOnUiThread {
                fm.executePendingTransactions()
                contFragment.startActivityForResult(intent, resultCode)
            }
        }
    }

}

private class ApplicationCoroutineScopeWrapper(val parent: CoroutineScope) :
        ContextedCoroutineScope<Context, ApplicationCoroutineScopeWrapper> {

    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "DEPRECATION")
    @Deprecated("Replace with top-level coroutineContext",
            replaceWith = ReplaceWith("coroutineContext",
                    imports = ["kotlin.coroutines.experimental.coroutineContext"]))
    @LowPriorityInOverloadResolution
    override val coroutineContext: CoroutineContext
        get() = parent.coroutineContext

    override val isActive: Boolean get() = parent.isActive

    @Suppress("UNCHECKED_CAST")
    suspend fun applicationContext(): ApplicationContext = extCoroutineContext[ApplicationContext]
            ?: throw IllegalStateException("Missing application context")

    override suspend fun getAndroidContext(): Context {
        return applicationContext().applicationContext
    }


    override suspend fun launch(context: CoroutineContext, start: CoroutineStart, parent: Job?, block: suspend ApplicationCoroutineScopeWrapper.() -> R): Job {
        return kotlinx.coroutines.experimental.launch(context + applicationContext(), start, parent) { ApplicationCoroutineScopeWrapper(this).block() }
    }

    override suspend fun <R> async(context: CoroutineContext, start: CoroutineStart, parent: Job?, block: suspend ApplicationCoroutineScopeWrapper.() -> R): Deferred<R> {
        return kotlinx.coroutines.experimental.async(context + applicationContext(), start, parent) { ApplicationCoroutineScopeWrapper(this).block() }
    }

}

private class DelegateLayoutContainer(override val containerView: View?) : LayoutContainer

private class ActivityCoroutineScopeWrapper<A : Activity>(parent: CoroutineScope) :
        LayoutContainerScopeWrapper<A, ActivityCoroutineScopeWrapper<A>>(parent), ActivityCoroutineScope<A, ActivityCoroutineScopeWrapper<A>> {

    override suspend fun launch(context: CoroutineContext, start: CoroutineStart, parent: Job?, block: suspend ActivityCoroutineScopeWrapper<A>.() -> R): Job {
        return kotlinx.coroutines.experimental.launch(context + activityContext(), start, parent) { ActivityCoroutineScopeWrapper<A>(this).block() }
    }

    override suspend fun <R> async(context: CoroutineContext, start: CoroutineStart, parent: Job?, block: suspend ActivityCoroutineScopeWrapper<A>.() -> R): Deferred<R> {
        return kotlinx.coroutines.experimental.async(context + activityContext(), start, parent) { ActivityCoroutineScopeWrapper<A>(this).block() }
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
        LayoutContainerScopeWrapper<Activity, FragmentCoroutineScope<F, Activity>>(parent), FragmentCoroutineScope<F, Activity> {

    constructor(parent: CoroutineScope, tag: String): this(parent, tag, 0)

    constructor(parent: CoroutineScope, id:Int): this(parent, null, id)

    @Suppress("UNCHECKED_CAST", "OverridingDeprecatedMember")
    override val fragment: F
        get() = activity.fragmentManager.run { tag?.let{ findFragmentByTag(it) } ?: findFragmentById(id) } as F

    @Suppress("UNCHECKED_CAST")
    override suspend fun fragment(): F {
        return activityContext().activity.fragmentManager.run { tag?.let{ findFragmentByTag(it) } ?: findFragmentById(id) } as F
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


    override suspend fun launch(context: CoroutineContext, start: CoroutineStart, parent: Job?, block: suspend FragmentCoroutineScope<F, Activity>.() -> R): Job {
        return kotlinx.coroutines.experimental.launch(context + activityContext(), start, parent) { createScopeWrapper(this).block() }
    }

    override suspend fun <R> async(context: CoroutineContext, start: CoroutineStart, parent: Job?, block: suspend FragmentCoroutineScope<F, Activity>.() -> R): Deferred<R> {
        return kotlinx.coroutines.experimental.async(context + activityContext(), start, parent) { createScopeWrapper(this).block() }
    }
}


class ActivityContext<A : Activity>(activity: A) : AbstractCoroutineContextElement(ActivityContext) {
    var activity = activity
        internal set

    companion object Key : CoroutineContext.Key<ActivityContext<*>>, Serializable

    override fun toString(): String = "ActivityContext"

}

class ApplicationContext(applicationContext: Context) : AbstractCoroutineContextElement(ApplicationContext) {
    var applicationContext: Context = applicationContext.applicationContext
        internal set

    companion object Key : CoroutineContext.Key<ApplicationContext>

    override fun toString(): String = "ApplicationContext"
}

interface ContextedCoroutineScope<out C : Context, out S : ContextedCoroutineScope<C, S>> : CoroutineScope {
    suspend fun getAndroidContext(): C

    suspend fun Account.hasFeatures(features: Array<String?>) = accountHasFeaturesImpl(this, features)

    suspend fun launch(context: CoroutineContext = DefaultDispatcher,
                       start: CoroutineStart = CoroutineStart.DEFAULT,
                       parent: Job? = null,
                       block: suspend S.() -> R): Job


    suspend fun <R> async(context: CoroutineContext = DefaultDispatcher,
                          start: CoroutineStart = CoroutineStart.DEFAULT,
                          parent: Job? = null,
                          block: suspend S.() -> R): Deferred<R>

}

interface LayoutContainerCoroutineScope<out A : Activity, out S : LayoutContainerCoroutineScope<A, S>> : ContextedCoroutineScope<A, S>, LayoutContainer {
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
    suspend fun startActivity(intent: Intent, options: Bundle) = activity().startActivity(intent, options)
}

@Suppress("DEPRECATION")
interface FragmentCoroutineScope<out F : Fragment, A : Activity> : LayoutContainerCoroutineScope<A, FragmentCoroutineScope<F, A>> {
    @Deprecated("Use function version")
    val fragment: F

    suspend fun fragment(): F
}

interface ActivityCoroutineScope<out A : Activity, out S : ActivityCoroutineScope<A, S>> : LayoutContainerCoroutineScope<A, S> {

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
suspend inline fun <reified A> FragmentCoroutineScope<*, *>.startActivityForResult() = startActivityForResult(Intent(activity(), A::class.java))

suspend inline fun <reified A> ActivityCoroutineScope<*, *>.startActivityForResult() = startActivityForResult(Intent(activity(), A::class.java))

inline fun <reified A> Activity.startActivityForResult(requestCode: Int) = this.startActivityForResult(Intent(this, A::class.java), requestCode)

@Suppress("unused", "DEPRECATION")
inline fun <reified A> Fragment.startActivityForResult(requestCode: Int) = this.startActivityForResult(Intent(activity, A::class.java), requestCode)

@Suppress("unused")
inline fun <reified A> Activity.startActivity() = startActivity(Intent(this, A::class.java))

@Suppress("unused", "DEPRECATION")
inline fun <reified A> Fragment.startActivity() = startActivity(Intent(activity, A::class.java))

