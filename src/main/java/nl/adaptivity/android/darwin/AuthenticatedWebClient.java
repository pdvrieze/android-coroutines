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

import android.Manifest.permission;
import android.accounts.*;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import nl.adaptivity.android.darwinlib.R;

import javax.net.ssl.HttpsURLConnection;

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.util.ArrayList;


/**
 * A class making it easier to make authenticated requests to darwin.
 */
public interface AuthenticatedWebClient {

  String KEY_ACCOUNT_NAME = "ACCOUNT_NAME";

  class InvalidAccountException extends RuntimeException {
    public InvalidAccountException() {
    }

    public InvalidAccountException(final String detailMessage) {
      super(detailMessage);
    }

    public InvalidAccountException(final Throwable throwable) {
      super(throwable);
    }

    public InvalidAccountException(final String detailMessage, final Throwable throwable) {
      super(detailMessage, throwable);
    }
  }

  class WebRequest {
    private final URI mUri;
    private String[] mHeaders;
    private int mHeaderCount;

    public WebRequest(final URI uri) {
      String uriScheme = uri.getScheme();
      if (! ("http".equals(uriScheme)|| "https".equals(uriScheme))) {
        throw new IllegalArgumentException("Webrequests only work with http and https schemes");
      }
      mUri = uri;
    }

    public void setHeader(final String name, final String value) {
      if (mHeaders==null) {
        mHeaders = new String[10];
      } else if (mHeaders.length==mHeaderCount*2) {
        String[] tmp = new String[mHeaders.length * 2];
        System.arraycopy(mHeaders, 0, tmp, 0, mHeaders.length);
        mHeaders = tmp;
      }
      int pos = mHeaderCount*2;
      mHeaders[pos] = name;
      mHeaders[pos+1] = value;
      mHeaderCount++;
    }

    public String getHeaderName(int index) {
      return mHeaders[index*2];
    }

    public String getHeaderValue(int index) {
      return mHeaders[index*2 + 1];
    }

    public int getHeaderCount() {
      return mHeaderCount;
    }

    public URI getUri() {
      return mUri;
    }

    @CallSuper
    public HttpURLConnection getConnection() throws IOException {
      HttpURLConnection connection = (HttpURLConnection) mUri.toURL().openConnection();
      for (int i = 0; i < mHeaderCount; i++) {
        connection.addRequestProperty(mHeaders[i * 2], mHeaders[i * 2 + 1]);
      }
      return connection;
    }
  }

  class GetRequest extends WebRequest {

    public GetRequest(final URI uri) {
      super(uri);
    }
  }

  class DeleteRequest extends WebRequest {

    public DeleteRequest(final URI uri) {
      super(uri);
    }

    @Override
    public HttpURLConnection getConnection() throws IOException {
      HttpURLConnection connection = super.getConnection();
      connection.setRequestMethod("DELETE");
      return connection;
    }
  }

  class CharSequenceCallback implements StreamWriterCallback {

    private final CharSequence mBody;

    public CharSequenceCallback(final CharSequence body) {
      mBody = body;
    }

    @Override
    public void writeTo(final OutputStream stream) throws IOException {
      Writer out = new OutputStreamWriter(stream, Charset.forName("UTF-8"));
      try {
        out.append(mBody);
      } finally {
        out.flush(); // Just flush, don't close.
      }
    }
  }

  class PostRequest extends WebRequest {

    private final StreamWriterCallback mWritingCallback;

    public PostRequest(final URI uri, StreamWriterCallback writingCallback) {
      super(uri);
      setContentType("application/binary"); // This is a much better default than form/urlencoded
      mWritingCallback = writingCallback;
    }

    public PostRequest(final URI uri, final CharSequence body) {
      this(uri, new CharSequenceCallback(body));
    }

    @Override
    public HttpURLConnection getConnection() throws IOException {
      HttpURLConnection connection = super.getConnection();
      connection.setDoOutput(true);
      connection.setChunkedStreamingMode(0);
      OutputStream outputStream = connection.getOutputStream();
      try {
        mWritingCallback.writeTo(outputStream);
      } finally {
        outputStream.close();
      }
      return connection;
    }

    public void setContentType(final String contentType) {
      setHeader("Content-type", contentType);
    }

  }

  interface StreamWriterCallback {
    void writeTo(OutputStream stream) throws IOException;
  }

  String ACCOUNT_TYPE = "uk.ac.bournemouth.darwin.account";

  String ACCOUNT_TOKEN_TYPE="uk.ac.bournemouth.darwin.auth";

  String KEY_ASKED_FOR_NEW = "askedForNewAccount";
  String KEY_AUTH_BASE = "authbase";

  String DARWIN_AUTH_COOKIE = "DWNID";
  String DOWNLOAD_DIALOG_TAG = "DOWNLOAD_DIALOG";
//  String _sharedPreferenceName = AuthenticatedWebClient.class.getName();

  HttpURLConnection execute(final WebRequest request) throws IOException;

  /**
   * Get the authentication base for this client
   */
  URI getAuthBase();

  /**
   * Get the stored account.
   * @return The account.
   */
  Account getAccount();

  /**
   * Record the used account name in a shared preferences file.
   * @param context The context to use to read the shared preferences.
   * @param accountname The name of the account.
   */
}
