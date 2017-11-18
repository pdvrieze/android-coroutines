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

import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.v4.util.SimpleArrayMap;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;
import android.util.Log;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;


/**
 * Class representing a directory of services.
 */
public class ServiceDirectory {

  private static final String TAG = "ServiceDirectory";
  private static final long CACHEVALIDMILIS = 24 * 60 * 60 * 1000; // 24 hours

  private final URI mLocation;
  private long mLookupTime;
  private SimpleArrayMap<String, Service> mServices;

  @Nullable
  public static ServiceDirectory fromPreferences(@NonNull SharedPreferences preferences, @NonNull String key) {
    if (!preferences.contains(key)) { return null; }
    String directoryDesc = preferences.getString(key, null);
    if (directoryDesc==null || directoryDesc.isEmpty()) {
      return null;
    }

    URI location = null;
    long lookupTime = 0L;
    SimpleArrayMap<String, Service> services = null;

    JsonReader jreader = new JsonReader(new StringReader(directoryDesc));
    try {
      if (jreader.peek() == JsonToken.BEGIN_OBJECT) {
        while (jreader.hasNext()) {
          switch (jreader.nextName()) {
            case "location": location = URI.create(jreader.nextString()); break;
            case "lookupTime": lookupTime = jreader.nextLong(); break;
            case "services": services = toServices(jreader); break;
          }
        }
      } else { // assume a single url
        return new ServiceDirectory(URI.create(directoryDesc));
      }
    } catch (IOException e) {
      Log.w(TAG, "Invalid service description", e);
      return null;
    }


    if (location ==null) { return null; }
    if (lookupTime==0L || services==null) { return new ServiceDirectory(location); }
    return new ServiceDirectory(location, lookupTime, services);
  }

  private static SimpleArrayMap<String, Service> toServices(JsonReader jReader) throws IOException {
    SimpleArrayMap<String, Service> services = new SimpleArrayMap<>(5);
    jReader.beginArray();
    while (jReader.hasNext()) {
      Service service = Service.fromJson(jReader);
      services.put(service.getName(), service);
    }
    jReader.endArray();
    return services;
  }

  private ServiceDirectory(final URI location, final long lookupTime, final SimpleArrayMap<String, Service> services) {
    mLocation = location;
    mLookupTime = lookupTime;
    mServices = services;

  }

  private ServiceDirectory(URI location) {
    mLocation = location;
    mServices = new SimpleArrayMap<>();
    mLookupTime=0L;
  }

  public Service getCachedService(String name) {
    long now = System.currentTimeMillis();
    return getCachedService(now, name);
  }

  private Service getCachedService(final long now, final String name) {
    Service service;
    if (now-CACHEVALIDMILIS>mLookupTime || (service=mServices.get(name))==null) {
      return null;
    }
    return service;
  }

  @WorkerThread
  Service getService(String name) throws IOException {
    long now = System.currentTimeMillis();
    Service service = getCachedService(now, name);
    if (service!=null) { return service; }
    if (mLocation==null) {
      return null;
    }
    URL url = new URL(mLocation.toString());
    JsonReader jreader = new JsonReader(new BufferedReader(new InputStreamReader(url.openStream(), Charset.forName("UTF-8"))));
    SimpleArrayMap<String, Service> services = toServices(jreader);
    mServices = services;
    mLookupTime = now;
    return mServices.get(name);
  }

  public void saveCache(@NonNull SharedPreferences.Editor editor, @NonNull String key) throws IOException {
    if (mLocation!=null) {
      StringWriter out = new StringWriter();
      JsonWriter writer = new JsonWriter(out);
      writer.beginObject();
      writer.name("location").value(mLocation.toString());
      writer.name("lookupTime").value(mLookupTime);
      writer.name("services").beginArray();
      for (int i = 0; i < mServices.size(); i++) {
        mServices.valueAt(i).toJson(writer);
      }
      writer.endArray();
      writer.endObject();
      writer.close();
      editor.putString(key, out.toString());
    }
  }

}
