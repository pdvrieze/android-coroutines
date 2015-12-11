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
import android.widget.Toast;
import nl.adaptivity.android.darwinlib.R;

import java.io.File;
import java.net.URI;


/**
 * A dialog fragment for downloading the authenticator
 */
public class DownloadDialog extends DialogFragment implements AlertDialog.OnClickListener {

  public static final String AUTHENTICATOR_URL = "https://darwin.bournemouth.ac.uk/darwin-auth.apk";
  private static final int INSTALL_ACTIVITY_REQUEST = 1234;
  private long mDownloadReference = -1L;
  private Uri mDownloaded;

  private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(final Context context, final Intent intent) {
      if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
        long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
        if (downloadId==mDownloadReference) {
          getActivity().unregisterReceiver(mBroadcastReceiver);
          DownloadManager downloadManager = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
          final Query query = new Query();
          query.setFilterById(mDownloadReference);
          Cursor data = downloadManager.query(query);
          try {
            if (data.moveToNext()) {
              int status = data.getInt(data.getColumnIndex(DownloadManager.COLUMN_STATUS));
              if (status==DownloadManager.STATUS_SUCCESSFUL) {
                mDownloaded = Uri.parse(data.getString(data.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)));
                doInstall(mDownloaded);
              }
            }
          } finally {
            data.close();
          }
        }
      }
    }
  };

  @Override
  public Dialog onCreateDialog(final Bundle savedInstanceState) {
    Builder builder = new Builder(getActivity());
    AlertDialog dialog = builder
            .setMessage(R.string.dlg_msg_confirm_authenticator_download)
            .setPositiveButton(R.string.btn_confirm_download, this)
            .setNegativeButton(android.R.string.no, this)
            .create();


    return super.onCreateDialog(savedInstanceState);
  }

  @Override
  public void onClick(final DialogInterface dialog, final int which) {
    if ( which == Activity.RESULT_OK) {
      doDownload();
      dialog.dismiss();
    } else {
      dialog.cancel();
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
    mDownloadReference = downloadManager.enqueue(request);
    getActivity().registerReceiver(mBroadcastReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
  }

  private void doInstall(final Uri uri) {
    File file = new File(URI.create(mDownloaded.toString()));
    file.setReadable(true, false);
    Intent installIntent = new Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive");
    startActivityForResult(installIntent, INSTALL_ACTIVITY_REQUEST);
  }

  @Override
  public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode==INSTALL_ACTIVITY_REQUEST) {
      File file = new File(URI.create(mDownloaded.toString()));
      file.delete();
      dismiss();
    }
  }
}
