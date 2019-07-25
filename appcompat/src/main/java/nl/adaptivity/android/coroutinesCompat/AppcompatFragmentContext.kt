package nl.adaptivity.android.coroutinesCompat

import android.support.v4.app.Fragment
import java.io.Serializable
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext


class AppcompatFragmentContext<F : Fragment>(fragment: F) :
    AbstractCoroutineContextElement(AppcompatFragmentContext) {
    var fragment = fragment
        internal set

    companion object Key : CoroutineContext.Key<AppcompatFragmentContext<*>>,
        Serializable

    override fun toString(): String = "AppcompatFragmentContext"

}