package nl.adaptivity.android.darwin

import android.app.Activity
import android.app.DownloadManager
import android.app.Fragment
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.suspendCancellableCoroutine
import nl.adaptivity.android.coroutines.Maybe
import nl.adaptivity.android.coroutines.ParcelableContinuation
import nl.adaptivity.android.darwinlib.R
import nl.adaptivity.android.kotlin.bundle
import nl.adaptivity.android.kotlin.set
import java.io.File
import java.net.URI
import kotlin.coroutines.experimental.Continuation

/**
 * Created by pdvrieze on 27/11/17.
 */
class DownloadFragment(): Fragment() {
    var downloadReference = -1L
    private var continuation: ParcelableContinuation<URI?>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { continuation = it.getParcelable(KEY_CONTINUATION) }
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.isActionDownloadComplete) {
                if (intent.downloadId == downloadReference) {
                    context.unregisterReceiver(this)
                    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    val query = DownloadManager.Query()
                    query.setFilterById(downloadReference)
                    downloadManager.query(query).use { data ->
                        val cont = continuation
                        if (data.moveToNext()) {
                            val status = data.getInt(DownloadManager.COLUMN_STATUS)
                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                cont?.resume(context, data.getUri(DownloadManager.COLUMN_LOCAL_URI))
                                continuation = null
                            } else if (status == DownloadManager.STATUS_FAILED) {
                                cont?.cancel(context)
                                continuation = null
                            }
                        }
                    }
                }
            }
        }
    }


    private fun doDownload(activity: Activity, downloadUri: Uri, description: String = "darwin-auth.apk", title: String = getString(R.string.download_title)) {
        val downloadManager = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        if (downloadReference >= 0) {
            val query = DownloadManager.Query()
            query.setFilterById(downloadReference)
            val data = downloadManager.query(query)
            if (data.moveToNext()) {
                val status = data.getInt(data.getColumnIndex(DownloadManager.COLUMN_STATUS))
                if (status == DownloadManager.STATUS_FAILED) {
                    downloadReference = -1
                } else {// do something better
                    Toast.makeText(activity, "Download already in progress", Toast.LENGTH_SHORT).show()
                }

            } else {
                downloadReference = -1
            }
        }
        val request = DownloadManager.Request(downloadUri).apply {
            setDescription(description)
            setTitle(title)
        }
        val cacheDir = activity.externalCacheDir
        val apkName = File(cacheDir, "darwin-auth.apk")
        if (apkName.exists()) {
            apkName.delete()
        }

        request.setDestinationUri(Uri.fromFile(apkName))
        downloadReference = downloadManager.enqueue(request)
        activity.registerReceiver(broadcastReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    companion object {
        private const val KEY_CONTINUATION ="_CONTINUATION_"
        private var fragNo = 0

        fun newInstance(continuation: Continuation<URI>): DownloadFragment {
            return DownloadFragment().apply {
                arguments = bundle { it[KEY_CONTINUATION] = ParcelableContinuation<URI>(continuation) }
            }
        }

        /**
         * Download the resource at [downloadUri] and return a URI of the local location
         */
        suspend fun download(activity: Activity, downloadUri: Uri): URI {
            return suspendCancellableCoroutine<URI> { cont ->
                val frag = newInstance(cont)
                activity.fragmentManager.beginTransaction().add(frag, nextTag()).commit()
                activity.runOnUiThread {
                    activity.fragmentManager.executePendingTransactions()
                    frag.doDownload(activity, downloadUri)
                }
            }
        }

        /**
         * Async version of [download] that has a callback instead of being a suspend function.
         */
        fun download(activity: Activity, downloadUri: Uri, callback: (Maybe<URI>) -> Unit) {
            launch {
                try {
                    download(activity, downloadUri).also { callback(Maybe.Ok(it)) }
                } catch (e: CancellationException) {
                    callback(Maybe.cancelled())
                } catch (e: Exception) {
                    callback(Maybe.error(e))
                }
            }
        }

        private fun nextTag(): String? {
            fragNo++
            return "__DOWNLOAD_FRAGMENT_$fragNo"
        }
    }

}