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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;
import nl.adaptivity.android.util.JsonUtil;

import java.io.IOException;
import java.net.URI;


/**
 * Created by pdvrieze on 11/03/16.
 */
public class Service {

  private static final String TAG = "Service";

  @NonNull private final String mName;
  @Nullable private final String mProtocol;
  @Nullable private final URI mUri;
  @Nullable private final String mExtra;

  public Service(@NonNull final String name, @Nullable final String protocol, @Nullable final URI uri, @Nullable final String extra) {
    mName = name;
    mProtocol = protocol;
    mUri = uri;
    mExtra = extra;
  }

  public void toJson(final JsonWriter writer) throws IOException {
    writer.name("name").value(mName);
    if (mProtocol!=null) { writer.name("protocol").value(mProtocol); }
    if (mUri!=null) { writer.name("url").value(mUri.toString()); }
    if (mExtra!=null) { writer.name("extra"); JsonUtil.writeFromString(writer, mExtra); }
  }

  public static Service fromJson(final JsonReader jreader) {
    try {
      String serviceName = null;
      String protocol = null;
      URI uri = null;
      String extra = null;

      jreader.beginObject();
      while (jreader.hasNext()) {
        String key = jreader.nextName();
        switch (key) {
          case "name":
            serviceName = jreader.nextString(); break;
          case "protocol":
            protocol = jreader.nextString(); break;
          case "url":
            uri = URI.create(jreader.nextString()); break;
          case "extra":
            extra = JsonUtil.readToString(jreader); break;
        }
      }
      return new Service(serviceName, protocol, uri, extra);
    } catch (IOException e) {
      Log.w(TAG, "Invalid service description", e);
    }
    return null;
  }

  public String getName() {
    return mName;
  }

  public String getProtocol() {
    return mProtocol;
  }

  public URI getResolvedUri(@NonNull URI baseUri) {
    if (mUri==null || mUri.isAbsolute()) {
      return mUri;
    }
    return baseUri.resolve(mUri);
  }

  public String getExtra() {
    return mExtra;
  }
}
