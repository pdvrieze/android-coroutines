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
    return launch(context + ActivityContext(this), start, parent) { ActivityCoroutineScopeWrapper<A>(this).block() }
}

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
fun <A : Activity, R> ActivityCoroutineScope<A, *>.aLaunch(context: CoroutineContext = DefaultDispatcher,
                                                           start: CoroutineStart = CoroutineStart.DEFAULT,
                                                           parent: Job? = null,
                                                           block: suspend ActivityCoroutineScope<A, *>.() -> R): Job {
    return kotlinx.coroutines.experimental.launch(context + coroutineContext[ActivityContext]!!, start, parent) {
        ActivityCoroutineScopeWrapper<A>(this@launch).block()
    }
}

/**
 * Version of the async function for android usage. It provides convenience access to context objects
 * in a safer way. The scope interface can also be the receiver of further convenience extensions.
 *
 * The function works analogous to [async].
 */
fun <A : Activity, R> ActivityCoroutineScope<A, *>.aAsync(context: CoroutineContext = DefaultDispatcher,
                                                          start: CoroutineStart = CoroutineStart.DEFAULT,
                                                          parent: Job? = null,
                                                          block: suspend ActivityCoroutineScope<A, *>.() -> R): Deferred<R> {

    return kotlinx.coroutines.experimental.async(context + coroutineContext[ActivityContext]!!, start, parent) { ActivityCoroutineScopeWrapper<A>(this).block() }
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
fun <F : Fragment, R> F.aLaunch(context: CoroutineContext = DefaultDispatcher,
                                start: CoroutineStart = CoroutineStart.DEFAULT,
                                parent: Job? = null,
                                block: suspend FragmentCoroutineScope<F, Activity>.() -> R): Job {
    return launch(context + ActivityContext(activity), start, parent) { FragmentCoroutineScopeWrapper<F>(this, tag).block() }
}

/**
 * Version of the async function for android usage. It provides convenience access to context objects
 * in a safer way. The scope interface can also be the receiver of further convenience extensions.
 *
 * The function works analogous to [async].
 */
fun <F : Fragment, R> F.aAsync(context: CoroutineContext = DefaultDispatcher,
                               start: CoroutineStart = CoroutineStart.DEFAULT,
                               parent: Job? = null,
                               block: suspend FragmentCoroutineScope<F, Activity>.() -> R): Deferred<R> {

    return async(context + ActivityContext(activity), start, parent) { FragmentCoroutineScopeWrapper<F>(this, tag).block() }
}

abstract class LayoutContainerScopeWrapper<out A : Activity, out S : LayoutContainerCoroutineScope<A, S>>(private val parent: CoroutineScope) : LayoutContainerCoroutineScope<A, S> {

    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "DEPRECATION")
    @Deprecated("Replace with top-level coroutineContext",
            replaceWith = ReplaceWith("coroutineContext",
                    imports = ["kotlin.coroutines.experimental.coroutineContext"]))
    @LowPriorityInOverloadResolution
    override val coroutineContext: CoroutineContext get() = parent.coroutineContext
    override val isActive: Boolean get() = parent.isActive

    override val containerView: View?
        get() = activity.window.decorView

    @Suppress("UNCHECKED_CAST")
    override val activity: A
        get() {
            // This is unsafe, but on creation the right activity should be set
            return coroutineContext.get(ActivityContext)?.activity as A
        }


    /**
     * Asynchronously invoke [Activity.startActivityForResult] returning the result on completion.
     */
    override suspend fun startActivityForResult(intent: Intent): ActivityResult {
        return suspendCoroutine { continuation ->
            val fm = fragmentManager
            val existingFragment = fm.findFragmentByTag(RetainedContinuationFragment.TAG) as RetainedContinuationFragment?
            val contFragment: RetainedContinuationFragment
            val resultCode: Int

            if (existingFragment != null) {
                resultCode = existingFragment.lastResultCode + 1
                existingFragment.addContinuation(ParcelableContinuation(continuation, activity, resultCode))
                contFragment = existingFragment
            } else {
                resultCode = COROUTINEFRAGMENT_RESULTCODE_START
                contFragment = RetainedContinuationFragment(ParcelableContinuation(continuation, activity, resultCode))


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
    override val coroutineContext: CoroutineContext get() = parent.coroutineContext

    override val isActive: Boolean get() = parent.isActive

    override fun getAndroidContext(): Context {
        return coroutineContext[ApplicationContext]!!.applicationContext
    }


    override fun launch(context: CoroutineContext, start: CoroutineStart, parent: Job?, block: suspend ApplicationCoroutineScopeWrapper.() -> R): Job {
        coroutineContext[ApplicationContext]!!.let { appContext ->
            return kotlinx.coroutines.experimental.launch(context + appContext, start, parent) { ApplicationCoroutineScopeWrapper(this).block() }
        }
    }

    override fun <R> async(context: CoroutineContext, start: CoroutineStart, parent: Job?, block: suspend ApplicationCoroutineScopeWrapper.() -> R): Deferred<R> {
        coroutineContext[ApplicationContext]!!.let { appContext ->
            return kotlinx.coroutines.experimental.async(context + appContext, start, parent) { ApplicationCoroutineScopeWrapper(this).block() }
        }
    }

}

private class ActivityCoroutineScopeWrapper<out A : Activity>(parent: CoroutineScope) :
        LayoutContainerScopeWrapper<A, ActivityCoroutineScopeWrapper<A>>(parent), ActivityCoroutineScope<A, ActivityCoroutineScopeWrapper<A>> {

    override fun launch(context: CoroutineContext, start: CoroutineStart, parent: Job?, block: suspend ActivityCoroutineScopeWrapper<A>.() -> R): Job {
        coroutineContext[ActivityContext]!!.let { activityContext ->
            return kotlinx.coroutines.experimental.launch(context + activityContext, start, parent) { ActivityCoroutineScopeWrapper<A>(this).block() }
        }
    }

    override fun <R> async(context: CoroutineContext, start: CoroutineStart, parent: Job?, block: suspend ActivityCoroutineScopeWrapper<A>.() -> R): Deferred<R> {
        coroutineContext[ActivityContext]!!.let { activityContext ->
            return kotlinx.coroutines.experimental.async(context + activityContext, start, parent) { ActivityCoroutineScopeWrapper<A>(this).block() }
        }
    }
}

private class FragmentCoroutineScopeWrapper<out F : Fragment>(parent: CoroutineScope, private val tag: String) :
        LayoutContainerScopeWrapper<Activity, FragmentCoroutineScope<F, Activity>>(parent), FragmentCoroutineScope<F, Activity> {
    @Suppress("UNCHECKED_CAST")
    override val fragment: F
        get() = activity.fragmentManager.findFragmentByTag(tag) as F

    override val fragmentManager: FragmentManager
        get() = fragment.fragmentManager


    override fun launch(context: CoroutineContext, start: CoroutineStart, parent: Job?, block: suspend FragmentCoroutineScope<F,Activity>.() -> R): Job {
        coroutineContext[ActivityContext]!!.let { activityContext ->
            return kotlinx.coroutines.experimental.launch(context + activityContext, start, parent) { FragmentCoroutineScopeWrapper<F>(this, tag).block() }
        }
    }

    override fun <R> async(context: CoroutineContext, start: CoroutineStart, parent: Job?, block: suspend FragmentCoroutineScope<F,Activity>.() -> R): Deferred<R> {
        coroutineContext[ActivityContext]!!.let { activityContext ->
            return kotlinx.coroutines.experimental.async(context + activityContext, start, parent) { FragmentCoroutineScopeWrapper<F>(this, tag).block() }
        }
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
    fun getAndroidContext(): C

    suspend fun Account.hasFeatures(features: Array<String?>) = accountHasFeaturesImpl(this, features)

    fun launch(context: CoroutineContext = DefaultDispatcher,
               start: CoroutineStart = CoroutineStart.DEFAULT,
               parent: Job? = null,
               block: suspend S.() -> R): Job


    fun <R> async(context: CoroutineContext = DefaultDispatcher,
                  start: CoroutineStart = CoroutineStart.DEFAULT,
                  parent: Job? = null,
                  block: suspend S.() -> R): Deferred<R>

}

interface LayoutContainerCoroutineScope<out A : Activity, out S : LayoutContainerCoroutineScope<A, S>> : ContextedCoroutineScope<A, S>, LayoutContainer {
    val activity: A
    val fragmentManager: FragmentManager

    override fun getAndroidContext() = activity

    fun <T : View> findViewById(@IdRes id: Int): T? = containerView?.findViewById(id)

    @RequiresPermission("android.permission.USE_CREDENTIALS")
    suspend fun Account.getAuthToken(authTokenType: String, options: Bundle? = null) =
            getAuthToken(this, authTokenType, options)

    suspend fun startActivityForResult(intent: Intent): ActivityResult

    fun startActivity(intent: Intent) = activity.startActivity(intent)

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    fun startActivity(intent: Intent, options: Bundle) = activity.startActivity(intent, options)
}

interface FragmentCoroutineScope<out F : Fragment, A : Activity> : LayoutContainerCoroutineScope<A, FragmentCoroutineScope<F, A>> {
    val fragment: F

}

interface ActivityCoroutineScope<out A : Activity, out S : ActivityCoroutineScope<A, S>> : LayoutContainerCoroutineScope<A, S> {

    override val fragmentManager: FragmentManager get() = activity.fragmentManager
}


inline suspend fun <reified A> ActivityCoroutineScope<*, *>.startActivityForResult() = startActivityForResult(Intent(activity, A::class.java))

inline fun <reified A> Activity.startActivityForResult(requestCode: Int) = this.startActivityForResult(Intent(this, A::class.java), requestCode)

inline fun <reified A> Activity.startActivity() = startActivity(Intent(this, A::class.java))

