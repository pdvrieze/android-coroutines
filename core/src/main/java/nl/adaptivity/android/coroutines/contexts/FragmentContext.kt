package nl.adaptivity.android.coroutines.contexts

import android.app.Fragment
import java.io.Serializable
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

class FragmentContext<F : Fragment>(fragment: F) :
    AbstractCoroutineContextElement(FragmentContext) {
    var fragment = fragment
        internal set

    companion object Key : CoroutineContext.Key<FragmentContext<*>>,
        Serializable

    override fun toString(): String = "FragmentContext"

}