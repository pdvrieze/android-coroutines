package nl.adaptivity.android.coroutinesCompat

import android.content.Intent
import nl.adaptivity.android.coroutines.FragmentCoroutineScopeWrapper
import android.support.v4.app.Fragment as SupportFragment


@Suppress("unused", "DEPRECATION")
inline fun <reified A> SupportFragment.startActivityForResult(requestCode: Int) = this.startActivityForResult(Intent(activity, A::class.java), requestCode)


@Suppress("unused", "DEPRECATION")
inline fun <reified A> SupportFragment.startActivity() = startActivity(Intent(activity, A::class.java))



@Suppress("unused")
suspend inline fun <reified A> AppcompatFragmentCoroutineScopeWrapper<*>.startActivityForResult() =
    startActivityForResult(Intent(fragment.activity, A::class.java))
