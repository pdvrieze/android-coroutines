package nl.adaptivity.android.coroutines

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import nl.adaptivity.android.coroutines.contexts.AndroidContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.launch as originalLaunch
import kotlinx.coroutines.async as originalAsync

abstract class WrappedContextCoroutineScope<out C : Context, out S : WrappedContextCoroutineScope<C, S>>(
    private val parentScope: CoroutineScope
) : AndroidContextCoroutineScope<C, S> {

    override fun getAndroidContext(): C = coroutineContext[AndroidContext] as C

    override fun <RES> async(
        context: CoroutineContext,
        start: CoroutineStart,
        block: suspend S.() -> RES
    ): Deferred<RES> {
        return originalAsync(
            context + coroutineContext[AndroidContext]!!,
            start
        ) { createScopeWrapper(this).block() }
    }

    override val coroutineContext: CoroutineContext
        get() = parentScope.coroutineContext

    suspend fun startActivityForResult(intent: Intent): ActivityResult {
        return suspendCoroutine { continuation ->
            val activity =
                (continuation.context[AndroidContext]?.androidContext
                    ?: throw IllegalStateException("Missing activity in context")) as Activity

            val contFragment: RetainedContinuationFragment =
                activity.ensureRetainingFragment()
            val resultCode: Int = contFragment.lastResultCode + 1

            contFragment.addContinuation(
                ParcelableContinuation(
                    continuation,
                    activity,
                    resultCode
                )
            )

            activity.runOnUiThread {
                contFragment.startActivityForResult(intent, resultCode)
            }
        }

    }


    fun startActivity(intent: Intent) = getAndroidContext().startActivity(intent)

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    fun startActivity(intent: Intent, options: Bundle) =
        getAndroidContext().startActivity(intent, options)

}