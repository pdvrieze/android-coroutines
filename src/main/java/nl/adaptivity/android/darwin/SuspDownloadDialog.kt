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
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.content.FileProvider
import android.widget.Toast
import nl.adaptivity.android.coroutines.SuspendableDialog
import nl.adaptivity.android.darwinlib.R

import java.io.File


/**
 * A dialog fragment for downloading the authenticator
 */
class SuspDownloadDialog : SuspendableDialog<Boolean>(), DialogInterface.OnClickListener {
    private var downloadReference = -1L
    private var downloaded: File? = null
    private var requestCode = INSTALL_ACTIVITY_REQUEST

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState?.containsKey(KEY_REQUEST_CODE)==true) {
            requestCode = savedInstanceState.getInt(KEY_REQUEST_CODE)
        } else {
            val args = arguments
            requestCode = args.getInt(KEY_REQUEST_CODE, INSTALL_ACTIVITY_REQUEST)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
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
            super.dispatchResult(true)
            dialog.dismiss()
        } else {
            super.dispatchResult(false)
            dialog.cancel()
        }
    }

    companion object {

        const val AUTHENTICATOR_URL = "https://darwin.bournemouth.ac.uk/darwin-auth.apk"
        private const val INSTALL_ACTIVITY_REQUEST = 1234
        const val KEY_REQUEST_CODE = "requestcode"

        @JvmStatic
        fun newInstance(requestCode: Int): SuspDownloadDialog {
            val result = SuspDownloadDialog()
            val b = Bundle(1)
            b.putInt(KEY_REQUEST_CODE, requestCode)
            result.arguments = b
            return result
        }
    }
}
