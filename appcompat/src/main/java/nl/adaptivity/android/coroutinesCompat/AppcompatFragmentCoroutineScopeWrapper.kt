package nl.adaptivity.android.coroutinesCompat

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.annotation.IdRes
import android.support.annotation.RequiresApi
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentManager
import android.view.View
import kotlinx.android.extensions.LayoutContainer
import kotlinx.coroutines.CoroutineScope
import nl.adaptivity.android.coroutines.ActivityResult
import nl.adaptivity.android.coroutines.ParcelableContinuation
import nl.adaptivity.android.coroutines.RetainedContinuationFragment
import nl.adaptivity.android.coroutines.contexts.FragmentContext
import nl.adaptivity.android.coroutines.ensureRetainingFragment
import nl.adaptivity.android.coroutines.impl.DelegateLayoutContainer
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.suspendCoroutine

class AppcompatFragmentCoroutineScopeWrapper<out F : Fragment>(
    private val parentScope: CoroutineScope
) : AppcompatFragmentCoroutineScope<F>, LayoutContainer {
    val activity: FragmentActivity? get() = fragment.activity

    override val fragment: F get() = coroutineContext[FragmentContext] as F

    val fragmentManager: FragmentManager? get() = fragment.fragmentManager

    override fun getAndroidContext() = activity

    override val containerView: View? get() = fragment.view

    fun <T : View> findViewById(@IdRes id: Int): T? = fragment.view?.findViewById(id)

    suspend fun startActivityForResult(intent: Intent): ActivityResult {
        return suspendCoroutine { continuation ->
            val fragment =
                (continuation.context[FragmentContext]?.fragment
                    ?: throw IllegalStateException("Missing fragment in context")) as Fragment

            val activity = activity
                ?: throw java.lang.IllegalStateException("The fragment must be attached to start another activity")

            val contFragment: RetainedContinuationFragment =
                activity.ensureRetainingFragment()
            val resultCode: Int = contFragment.lastResultCode + 1

            contFragment.addContinuation(
                ParcelableContinuation(
                    continuation,
                    fragment.activity,
                    resultCode
                )
            )

            activity.runOnUiThread {
                contFragment.startActivityForResult(intent, resultCode)
            }
        }

    }

    fun startActivity(intent: Intent) = fragment.startActivity(intent)

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    fun startActivity(intent: Intent, options: Bundle) =
        fragment.startActivity(intent, options)


    @Suppress("unused")
    suspend fun <R> layoutContainer(body: LayoutContainer.() -> R): R {
        return DelegateLayoutContainer(fragment.view).body()
    }

    @Suppress("unused")
    inline fun <R> withFragment(body: F.() -> R): R {
        return fragment.body()
    }

    override val coroutineContext: CoroutineContext get() = parentScope.coroutineContext
}