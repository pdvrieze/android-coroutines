@file:UseExperimental(ExperimentalTypeInference::class)

package nl.adaptivity.android.coroutines

import android.app.Activity
import android.app.Fragment
import android.content.Intent
import kotlin.experimental.ExperimentalTypeInference

fun Activity.ensureRetainingFragment(): RetainedContinuationFragment {
    val fm = fragmentManager
    val existingFragment =
        fm.findFragmentByTag(RetainedContinuationFragment.TAG) as RetainedContinuationFragment?

    if (existingFragment != null) return existingFragment

    val contFragment = RetainedContinuationFragment()
    fm.beginTransaction().apply {
        // This shouldn't happen, but in that case remove the old continuation.
        existingFragment?.let { remove(it) }

        add(contFragment, RetainedContinuationFragment.TAG)
    }.commit()
    runOnUiThread { fm.executePendingTransactions() }

    return contFragment
}

@Suppress("unused")
suspend inline fun <reified A> FragmentCoroutineScopeWrapper<*>.startActivityForResult() =
    startActivityForResult(Intent(fragment.activity, A::class.java))

suspend inline fun <reified A> WrappedContextCoroutineScope<Activity, *>.startActivityForResult(): ActivityResult =
    startActivityForResult(Intent(getAndroidContext(), A::class.java))

inline fun <reified A> Activity.startActivityForResult(requestCode: Int) =
    this.startActivityForResult(Intent(this, A::class.java), requestCode)

@Suppress("unused", "DEPRECATION")
inline fun <reified A> Fragment.startActivityForResult(requestCode: Int) =
    this.startActivityForResult(Intent(activity, A::class.java), requestCode)

@Suppress("unused")
inline fun <reified A> Activity.startActivity() = startActivity(Intent(this, A::class.java))

@Suppress("unused", "DEPRECATION")
inline fun <reified A> Fragment.startActivity() = startActivity(Intent(activity, A::class.java))

