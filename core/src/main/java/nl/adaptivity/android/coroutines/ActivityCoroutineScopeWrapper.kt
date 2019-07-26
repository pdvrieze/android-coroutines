package nl.adaptivity.android.coroutines

import android.app.Activity
import android.app.FragmentManager
import android.support.annotation.IdRes
import android.view.View
import kotlinx.android.extensions.LayoutContainer
import kotlinx.coroutines.CoroutineScope
import nl.adaptivity.android.coroutines.contexts.AndroidContext
import nl.adaptivity.android.coroutines.impl.DelegateLayoutContainer

class ActivityCoroutineScopeWrapper<out A : Activity>(
    parentScope: CoroutineScope
) :
    WrappedContextCoroutineScope<A, ActivityCoroutineScopeWrapper<A>>(parentScope),
    LayoutContainer {

    @Suppress("UNCHECKED_CAST")
    val activity: A
        get() = coroutineContext[AndroidContext]!!.androidContext as A

    @Suppress("DEPRECATION")
    @Deprecated("Use function", ReplaceWith("fragmentManager()"))
    val fragmentManager: FragmentManager
        get() = activity.fragmentManager

    @Suppress("DEPRECATION")
    fun fragmentManager(): FragmentManager = fragmentManager()

    override fun getAndroidContext() = activity

    override val containerView: View? get() = activity.findViewById(android.R.id.content)

    fun <T : View> findViewById(@IdRes id: Int): T = activity.findViewById(id)

    override fun createScopeWrapper(parentScope: CoroutineScope): ActivityCoroutineScopeWrapper<A> {
        return ActivityCoroutineScopeWrapper(parentScope)
    }

    @Suppress("unused")
    suspend fun <R> layoutContainer(body: LayoutContainer.() -> R): R {
        return DelegateLayoutContainer(activity.window.decorView)
            .body()
    }

    @Suppress("unused")
    suspend inline fun <R> withActivity(body: A.() -> R): R {
        return activity.body()
    }

}