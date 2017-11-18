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

import android.content.SharedPreferences
import android.support.annotation.WorkerThread
import android.support.v4.util.SimpleArrayMap
import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import android.util.Log

import java.io.*
import java.net.URI
import java.net.URL
import java.nio.charset.Charset


@Suppress("unused")
/**
 * Class representing a directory of services.
 */
class ServiceDirectory private constructor(private val location: URI?, private var lookupTime: Long = 0, private var services: SimpleArrayMap<String, Service>? = null) {

    fun getCachedService(name: String): Service? {
        val now = System.currentTimeMillis()
        return getCachedService(now, name)
    }

    private fun getCachedService(now: Long, name: String): Service? = when {
        now - CACHEVALIDMILIS > lookupTime -> null
        else -> services?.get(name)
    }

    @WorkerThread
    @Throws(IOException::class)
    internal fun getService(name: String): Service? {
        val now = System.currentTimeMillis()
        getCachedService(now, name)?.also { cachedService -> return cachedService }

        if (location == null) {
            return null
        }
        val url = URL(location.toString())
        val jreader = JsonReader(BufferedReader(InputStreamReader(url.openStream(), Charset.forName("UTF-8"))))
        val services = toServices(jreader)
        this.services = services
        lookupTime = now
        return services.get(name)
    }

    @Throws(IOException::class)
    fun saveCache(editor: SharedPreferences.Editor, key: String) {
        val services = this.services
        if (location != null && services!=null) {
            val out = StringWriter()
            val writer = JsonWriter(out)
            writer.beginObject()
            writer.name("location").value(location.toString())
            writer.name("lookupTime").value(lookupTime)
            writer.name("services").beginArray()
            for (i in 0 until services.size()) {
                services.valueAt(i).toJson(writer)
            }
            writer.endArray()
            writer.endObject()
            writer.close()
            editor.putString(key, out.toString())
        }
    }

    companion object {

        private const val TAG = "ServiceDirectory"
        private const val CACHEVALIDMILIS = (24 * 60 * 60 * 1000).toLong() // 24 hours

        fun fromPreferences(preferences: SharedPreferences, key: String): ServiceDirectory? {
            if (!preferences.contains(key)) {
                return null
            }
            val directoryDesc = preferences.getString(key, null)
            if (directoryDesc?.isEmpty()!=false) return null

            var location: URI? = null
            var lookupTime = 0L
            var services: SimpleArrayMap<String, Service>? = null

            val jreader = JsonReader(StringReader(directoryDesc))
            try {
                if (jreader.peek() == JsonToken.BEGIN_OBJECT) {
                    while (jreader.hasNext()) {
                        when (jreader.nextName()) {
                            "location" -> location = URI.create(jreader.nextString())
                            "lookupTime" -> lookupTime = jreader.nextLong()
                            "services" -> services = toServices(jreader)
                        }
                    }
                } else { // assume a single url
                    return ServiceDirectory(URI.create(directoryDesc))
                }
            } catch (e: IOException) {
                Log.w(TAG, "Invalid service description", e)
                return null
            }


            if (location == null) {
                return null
            }
            return if (lookupTime == 0L || services == null) {
                ServiceDirectory(location)
            } else ServiceDirectory(location, lookupTime, services)
        }

        @Throws(IOException::class)
        private fun toServices(jReader: JsonReader): SimpleArrayMap<String, Service> {
            val services = SimpleArrayMap<String, Service>(5)
            jReader.beginArray()
            while (jReader.hasNext()) {
                val service = Service.fromJson(jReader)
                services.put(service!!.name, service)
            }
            jReader.endArray()
            return services
        }
    }

}
