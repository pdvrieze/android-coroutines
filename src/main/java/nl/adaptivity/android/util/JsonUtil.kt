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

@file:JvmName("JsonUtil")
package nl.adaptivity.android.util

import android.util.JsonReader
import android.util.JsonWriter
import android.util.JsonToken

import java.io.IOException
import java.io.StringReader
import java.io.StringWriter

inline fun <R> JsonWriter.inArray(block: ()->R): R {
    beginArray()
    return block().also {
        endArray()
    }
}

inline fun <R> JsonReader.inArray(block: ()->R): R {
    beginArray()
    return block().also {
        endArray()
    }
}

inline fun <R> JsonWriter.inObject(block: ()->R): R {
    beginObject()
    return block().also {
        endObject()
    }
}

inline fun <R> JsonReader.inObject(block: ()->R): R {
    beginObject()
    return block().also {
        endObject()
    }
}

@Throws(IOException::class)
@JvmName("readToWriter")
fun JsonReader.writeTo(out: JsonWriter) {
    val reader = this
    while (reader.hasNext()) {
        when (reader.peek()) {
            JsonToken.BEGIN_ARRAY -> {
                out.inArray {
                    reader.inArray {
                        reader.writeTo(out)
                    }
                }
            }
            JsonToken.BEGIN_OBJECT -> {
                out.inObject { reader.inObject {
                    reader.writeTo(out)
                } }
            }
            JsonToken.BOOLEAN -> out.value(reader.nextBoolean())
            JsonToken.NAME -> out.name(reader.nextName())
            JsonToken.NULL -> {
                reader.nextNull()
                out.nullValue()
            }
            JsonToken.NUMBER -> {
                val numberString = reader.nextString()
                try {
                    out.value(java.lang.Long.parseLong(numberString))
                } catch (e: NumberFormatException) {
                    out.value(java.lang.Double.parseDouble(numberString))
                }

            }
            JsonToken.STRING -> out.value(reader.nextString())
        }
    }
}

@Throws(IOException::class)
fun readToString(reader: JsonReader): String {
    val result = StringWriter()
    JsonWriter(result).use { out ->
        reader.writeTo(out)
    }
    return result.toString()
}

@Throws(IOException::class)
fun writeFromString(out: JsonWriter, jsonString: String) {
    JsonReader(StringReader(jsonString)).writeTo(out)
}
