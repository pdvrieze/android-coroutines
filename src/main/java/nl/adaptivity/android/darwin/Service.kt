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

import android.util.JsonReader
import android.util.JsonWriter
import android.util.Log
import nl.adaptivity.android.util.readToString
import nl.adaptivity.android.util.writeFromString
import java.io.IOException
import java.net.URI


/**
 * Class representing the information needed for retrieving service locations.
 *
 * @property name The name of the service. This is used to look up the service, possibly with the
 *                protocol
 * @property protocol The protocol the service uses
 * @property uri The location of the service
 * @property extra Any additional information
 */
class Service(val name: String, val protocol: String?, private val uri: URI?, val extra: String?) {

    /**
     * Get a JSon representation of the service.
     *
     * @param writer The writer to write the representation to.
     */
    @Throws(IOException::class)
    fun toJson(writer: JsonWriter) {
        writer.name("name").value(name)
        protocol?.let { writer.name("protocol").value(it) }
        uri?.let { writer.name("url").value(it.toString()) }
        extra?.let {
            writer.name("extra")
            writeFromString(writer, it)
        }
    }

    /**
     * Get the uri as a resolved uri based on the input parameter.
     */
    fun getResolvedUri(baseUri: URI): URI? {
        return if (uri == null || uri.isAbsolute) {
            uri
        } else baseUri.resolve(uri)
    }

    companion object {

        private const val TAG = "Service"

        /**
         * Get a service from the json representation in the reader.
         *
         * @param jreader The [JsonReader] to read the service from.
         */
        @JvmStatic
        fun fromJson(jreader: JsonReader): Service? {
            try {
                var serviceName: String? = null
                var protocol: String? = null
                var uri: URI? = null
                var extra: String? = null

                jreader.beginObject()
                while (jreader.hasNext()) {
                    val key = jreader.nextName()
                    when (key) {
                        "name" -> serviceName = jreader.nextString()
                        "protocol" -> protocol = jreader.nextString()
                        "url" -> uri = URI.create(jreader.nextString())
                        "extra" -> extra = readToString(jreader)
                    }
                }
                if (serviceName == null) throw IllegalArgumentException("No service name was provided")
                return Service(serviceName, protocol, uri, extra)
            } catch (e: IOException) {
                Log.w(TAG, "Invalid service description", e)
            }

            return null
        }
    }
}
