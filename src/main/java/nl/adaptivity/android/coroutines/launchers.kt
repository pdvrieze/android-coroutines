package nl.adaptivity.android.coroutines

import android.app.Activity
import android.app.FragmentManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.annotation.IdRes
import android.support.annotation.RequiresApi
import android.view.View
import kotlinx.android.extensions.LayoutContainer
import kotlinx.coroutines.experimental.*
import java.io.Serializable
import kotlin.coroutines.experimental.AbstractCoroutineContextElement
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.suspendCoroutine

fun <A : Activity, R> A.aLaunch(context: CoroutineContext, start: CoroutineStart, parent: Job? = null, block: suspend ActivityCoroutineScope<A>.() -> R): Job {
    return launch(context + ActivityContext(this), start, parent) { ActivityCoroutineScopeWrapper<A>(this).block() }
}

fun <A : Activity, R> A.aAsync(context: CoroutineContext = DefaultDispatcher,
                               start: CoroutineStart = CoroutineStart.DEFAULT,
                               parent: Job? = null,
                               block: suspend ActivityCoroutineScope<A>.() -> R): Deferred<R> {

    return async(context + ActivityContext(this), start, parent) { ActivityCoroutineScopeWrapper<A>(this).block() }
}

class ActivityCoroutineScopeWrapper<out A : Activity>(private val parent: CoroutineScope) : ActivityCoroutineScope<A> {

    override val containerView: View?
        get() = activity.window.decorView

    override val activity: A
        get() {
            return coroutineContext.get(ActivityContext)?.activity as A
        }

    override val context: CoroutineContext get() = parent.context
    override val isActive: Boolean get() = parent.isActive


    /**
     * Asynchronously invoke [Activity.startActivityForResult] returning the result on completion.
     */
    override suspend fun startActivityForResult(intent: Intent): ActivityResult {
        return suspendCoroutine { continuation ->
            val fm = activity.fragmentManager
            val contFragment = RetainedContinuationFragment(ParcelableContinuation(continuation, activity, COROUTINEFRAGMENT_RESULTCODE_START))

            fm.beginTransaction().apply {
                // This shouldn't happen, but in that case remove the old continuation.
                fm.findFragmentByTag(RetainedContinuationFragment.TAG)?.let { remove(it) }

                add(contFragment, RetainedContinuationFragment.TAG)
            }.commit()


            activity.runOnUiThread {
                fm.executePendingTransactions()
                contFragment.startActivityForResult(intent, COROUTINEFRAGMENT_RESULTCODE_START)
            }
        }
    }

}


class ActivityContext<A : Activity>(activity: A) : AbstractCoroutineContextElement(ActivityContext) {
    var activity = activity
        internal set

    companion object Key : CoroutineContext.Key<ActivityContext<*>>, Serializable

    override fun toString(): String = "ActivityContext"

}

interface ActivityCoroutineScope<out A : Activity> : CoroutineScope, LayoutContainer {
    val activity: A
    val fragmentManager: FragmentManager get() = activity.fragmentManager

    suspend fun startActivityForResult(intent: Intent): ActivityResult

    fun startActivity(intent: Intent) = activity.startActivity(intent)

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    fun startActivity(intent: Intent, options: Bundle) = activity.startActivity(intent, options)

    fun <T:View> findViewById(@IdRes id: Int):T = activity.window.findViewById(id)
}


inline suspend fun <reified A> ActivityCoroutineScope<*>.startActivityForResult() = startActivityForResult(Intent(activity, A::class.java))

inline fun <reified A> Activity.startActivityForResult(requestCode:Int) = this.startActivityForResult(Intent(this, A::class.java), requestCode)

inline fun <reified A> Activity.startActivity() = startActivity(Intent(this, A::class.java))

