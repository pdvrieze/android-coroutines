package nl.adaptivity.android.coroutines

import android.app.Activity
import android.app.DownloadManager
import android.app.Fragment
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.net.URI
import kotlin.coroutines.Continuation

/**
 * Fragment that encapsulates the state of downloading a file.
 *
 * TODO Actually handle the case where download completed when the activity is in the background.
 */
class DownloadFragment(): Fragment() {
    var downloadReference = -1L
    private var continuation: ParcelableContinuation<URI?>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { continuation = it.getParcelable(KEY_CONTINUATION) }
        savedInstanceState?.apply { downloadReference = getLong(KEY_DOWNLOAD_REFERENCE, -1L) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_DOWNLOAD_REFERENCE, downloadReference)
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


    private fun doDownload(activity: Activity, downloadUri: Uri, fileName: String, description: String = fileName, title: String = fileName) {
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
        val downloadFile = File(cacheDir, fileName)
        if (downloadFile.exists()) {
            downloadFile.delete()
        }

        request.setDestinationUri(Uri.fromFile(downloadFile))
        downloadReference = downloadManager.enqueue(request)
        activity.registerReceiver(broadcastReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    companion object {
        private const val KEY_DOWNLOAD_REFERENCE = "DOWNLOAD_REFERENCE"
        private const val KEY_CONTINUATION = "_CONTINUATION_"
        private var fragNo = 0

        /**
         * Create a new instance of the fragment with the given continuation as parameter.
         */
        @Deprecated("This should be private. Use download directly instead", level = DeprecationLevel.WARNING)
        fun newInstance(continuation: Continuation<URI>): DownloadFragment {
            return DownloadFragment().apply {
                arguments = Bundle(1).apply { putParcelable(KEY_CONTINUATION, ParcelableContinuation<URI>(continuation, activity)) }
            }
        }

        /**
         * Download the resource at [downloadUri] and return a URI of the local location
         */
        suspend fun download(activity: Activity, downloadUri: Uri): URI {
            return suspendCancellableCoroutine<URI> { cont ->
                @Suppress("DEPRECATION")
                val frag = newInstance(cont)
                activity.fragmentManager.beginTransaction().add(frag, nextTag()).commit()
                activity.runOnUiThread {
                    activity.fragmentManager.executePendingTransactions()
                    frag.doDownload(activity, downloadUri, fileName = "darwin-auth.apk")
                }
            }
        }

        /**
         * Async version of [download] that has a callback instead of being a suspend function.
         */
        @JvmStatic
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

/* Helper function to get an integer by name from a cursor. */
private fun Cursor.getInt(columnName:String) = getInt(getColumnIndex(columnName))
/* Helper function to get a string by name from a cursor. */
private fun Cursor.getString(columnName:String) = getString(getColumnIndex(columnName))
/* Helper function to get an uri by name from a cursor. */
private fun Cursor.getUri(columnName:String) = URI.create(getString(getColumnIndex(columnName)))

private inline var Intent.downloadId: Long
    get() = getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
    set(value) { extras.putLong(DownloadManager.EXTRA_DOWNLOAD_ID, value) }

private inline val Intent.isActionDownloadComplete get() = action == DownloadManager.ACTION_DOWNLOAD_COMPLETE
