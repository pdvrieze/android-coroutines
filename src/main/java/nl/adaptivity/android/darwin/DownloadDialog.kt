/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 2.1 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.android.darwin

import android.app.*
import android.app.AlertDialog.Builder
import android.app.DownloadManager.Query
import android.app.DownloadManager.Request
import android.content.*
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.content.FileProvider
import android.widget.Toast
import nl.adaptivity.android.darwinlib.R

import java.io.File
import java.net.URI


internal inline var Intent.downloadId: Long
    get() = getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
    set(value) { extras.putLong(DownloadManager.EXTRA_DOWNLOAD_ID, value) }

internal inline val Intent.isActionDownloadComplete get() = action == DownloadManager.ACTION_DOWNLOAD_COMPLETE

internal fun Cursor.getInt(columnName:String) = getInt(getColumnIndex(columnName))
internal fun Cursor.getString(columnName:String) = getString(getColumnIndex(columnName))
internal fun Cursor.getUri(columnName:String) = URI.create(getString(getColumnIndex(columnName)))

/**
 * A dialog fragment for downloading the authenticator
 */
class DownloadDialog : DialogFragment(), DialogInterface.OnClickListener {
    private var downloadReference = -1L
    private var downloaded: File? = null
    private var requestCode = INSTALL_ACTIVITY_REQUEST

    private val mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.isActionDownloadComplete) {
                if (intent.downloadId == downloadReference) {
                    context.unregisterReceiver(this)
                    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    val query = Query()
                    query.setFilterById(downloadReference)
                    downloadManager.query(query).use { data ->
                        if (data.moveToNext()) {

                            if (data.getInt(DownloadManager.COLUMN_STATUS) == DownloadManager.STATUS_SUCCESSFUL) {

                                downloaded = File(data.getUri(DownloadManager.COLUMN_LOCAL_URI))

                                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
                                    doInstall(context, FileProvider.getUriForFile(context, "uk.ac.bmth.scitech.aprog.fileProvider", downloaded))
                                } else {
                                    doInstall(context, Uri.fromFile(downloaded))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState?.containsKey(KEY_REQUEST_CODE)==true) {
            requestCode = savedInstanceState.getInt(KEY_REQUEST_CODE)
        } else {
            val args = arguments
            requestCode = args.getInt(KEY_REQUEST_CODE, INSTALL_ACTIVITY_REQUEST)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle): Dialog {
        val args = arguments
        requestCode = args.getInt(KEY_REQUEST_CODE, INSTALL_ACTIVITY_REQUEST)


        val builder = Builder(activity)


        return builder
                .setMessage(R.string.dlg_msg_confirm_authenticator_download)
                .setPositiveButton(R.string.btn_confirm_download, this)
                .setNegativeButton(android.R.string.no, this)
                .create()
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        if (which == Activity.RESULT_OK) {
            doDownload()
            dialog.dismiss()
        } else {
            dialog.cancel()

            (activity as? AuthenticatedWebClientFactory.AuthenticatedWebClientCallbacks)?.onDownloadCancelled()
        }
    }

    private fun doDownload() {
        val downloadManager = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        if (downloadReference >= 0) {
            val query = Query()
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
        val request = Request(Uri.parse(AUTHENTICATOR_URL)).apply {
            setDescription("darwin-auth.apk")
            setTitle(getString(R.string.download_title))
        }
        val cacheDir = activity.externalCacheDir
        val apkName = File(cacheDir, "darwin-auth.apk")
        if (apkName.exists()) {
            apkName.delete()
        }

        request.setDestinationUri(Uri.fromFile(apkName))
        downloadReference = downloadManager.enqueue(request)
        activity.registerReceiver(mBroadcastReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    private fun doInstall(context: Context, uri: Uri) {
        //    file.setReadable(true, false);
        val installIntent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
        installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (context is Activity) {
            context.startActivityForResult(installIntent, requestCode)
        } else {
            context.startActivity(installIntent)
        }
        dismiss()
    }

    companion object {

        const val AUTHENTICATOR_URL = "https://darwin.bournemouth.ac.uk/darwin-auth.apk"
        private const val INSTALL_ACTIVITY_REQUEST = 1234
        const val KEY_REQUEST_CODE = "requestcode"

        @JvmStatic
        fun newInstance(requestCode: Int): DownloadDialog {
            val result = DownloadDialog()
            val b = Bundle(1)
            b.putInt(KEY_REQUEST_CODE, requestCode)
            result.arguments = b
            return result
        }
    }
}
