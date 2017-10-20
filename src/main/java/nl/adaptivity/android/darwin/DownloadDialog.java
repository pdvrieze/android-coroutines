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

package nl.adaptivity.android.darwin;

import android.app.*;
import android.app.AlertDialog.Builder;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.widget.Toast;
import nl.adaptivity.android.darwinlib.BuildConfig;
import nl.adaptivity.android.darwinlib.R;

import java.io.File;
import java.net.URI;


/**
 * A dialog fragment for downloading the authenticator
 */
public class DownloadDialog extends DialogFragment implements AlertDialog.OnClickListener {

  public static final String AUTHENTICATOR_URL = "https://darwin.bournemouth.ac.uk/darwin-auth.apk";
  private static final int INSTALL_ACTIVITY_REQUEST = 1234;
  public static final java.lang.String KEY_REQUEST_CODE = "requestcode";
  private long mDownloadReference = -1L;
  private File mDownloaded;
  private int mRequestCode = INSTALL_ACTIVITY_REQUEST;

  private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(final Context context, final Intent intent) {
      if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
        long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
        if (downloadId==mDownloadReference) {
          context.unregisterReceiver(mBroadcastReceiver);
          DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
          final Query query = new Query();
          query.setFilterById(mDownloadReference);
          Cursor data = downloadManager.query(query);
          try {
            if (data.moveToNext()) {
              int status = data.getInt(data.getColumnIndex(DownloadManager.COLUMN_STATUS));
              if (status==DownloadManager.STATUS_SUCCESSFUL) {
                mDownloaded = new File(URI.create(data.getString(data.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))));
                String authority = BuildConfig.APPLICATION_ID+".darwinlib.fileProvider";
                doInstall(context, FileProvider.getUriForFile(context, authority, mDownloaded));
              }
            }
          } finally {
            data.close();
          }
        }
      }
    }
  };

  public static DownloadDialog newInstance(int requestCode) {
    DownloadDialog result = new DownloadDialog();
    Bundle b = new Bundle(1);
    b.putInt(KEY_REQUEST_CODE, requestCode);
    result.setArguments(b);
    return result;
  }

  @Override
  public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (savedInstanceState!=null && savedInstanceState.containsKey(KEY_REQUEST_CODE)) {
      mRequestCode = savedInstanceState.getInt(KEY_REQUEST_CODE);
    } else {
      final Bundle args = getArguments();
      mRequestCode = args.getInt(KEY_REQUEST_CODE, INSTALL_ACTIVITY_REQUEST);
    }
  }

  @Override
  public Dialog onCreateDialog(final Bundle savedInstanceState) {
    final Bundle args = getArguments();
    mRequestCode = args.getInt(KEY_REQUEST_CODE, INSTALL_ACTIVITY_REQUEST);


    Builder builder = new Builder(getActivity());
    AlertDialog dialog = builder
            .setMessage(R.string.dlg_msg_confirm_authenticator_download)
            .setPositiveButton(R.string.btn_confirm_download, this)
            .setNegativeButton(android.R.string.no, this)
            .create();


    return dialog;
  }

  @Override
  public void onClick(final DialogInterface dialog, final int which) {
    if ( which == Activity.RESULT_OK) {
      doDownload();
      dialog.dismiss();
    } else {
      dialog.cancel();
      if(getActivity() instanceof AuthenticatedWebClientFactory.AuthenticatedWebClientCallbacks) {
        ((AuthenticatedWebClientFactory.AuthenticatedWebClientCallbacks)getActivity()).onDownloadCancelled();
      }
    }
  }

  private void doDownload() {
    DownloadManager downloadManager = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
    if (mDownloadReference>=0) {
      final Query query = new Query();
      query.setFilterById(mDownloadReference);
      Cursor data = downloadManager.query(query);
      if (data.moveToNext()) {
        int status = data.getInt(data.getColumnIndex(DownloadManager.COLUMN_STATUS));
        if (status == DownloadManager.STATUS_FAILED) {
          mDownloadReference = -1;
        } else {// do something better
          Toast.makeText(getActivity(), "Download already in progress", Toast.LENGTH_SHORT).show();
        }

      } else {
        mDownloadReference = -1;
      }
    }
    Request request = new Request(Uri.parse(AUTHENTICATOR_URL));
    request.setDescription("darwin-auth.apk");
    request.setTitle(getString(R.string.download_title));
    File cacheDir = getActivity().getExternalCacheDir();
    File apkName = new File(cacheDir, "darwin-auth.apk");
    if (apkName.exists()) {
      apkName.delete();
    }

    request.setDestinationUri(Uri.fromFile(apkName));
    mDownloadReference = downloadManager.enqueue(request);
    getActivity().registerReceiver(mBroadcastReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
  }

  private void doInstall(final Context context, final Uri uri) {
//    file.setReadable(true, false);
    Intent installIntent = new Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive");
    installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    if (context instanceof Activity) {
      ((Activity) context).startActivityForResult(installIntent, mRequestCode);
    } else {
      context.startActivity(installIntent);
    }
    dismiss();
  }
}
