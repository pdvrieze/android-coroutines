package nl.adaptivity.android.coroutines.contexts

import android.content.Context
import java.io.Serializable
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

class AndroidContext<C : Context>(androidContext: C) :
    AbstractCoroutineContextElement(AndroidContext) {
    var androidContext = androidContext
        internal set

    companion object Key : CoroutineContext.Key<AndroidContext<*>>,
        Serializable

    override fun toString(): String = "AndroidContext"

}