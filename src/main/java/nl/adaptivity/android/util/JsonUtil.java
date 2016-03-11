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

package nl.adaptivity.android.util;

import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;


/**
 * Created by pdvrieze on 11/03/16.
 */
public final class JsonUtil {

  private JsonUtil() {}

  public static void readToWriter(final JsonReader in, final JsonWriter out) throws IOException {
    while(in.hasNext()) {
      switch (in.peek()) {
        case BEGIN_ARRAY:
          out.beginArray();
          in.beginArray();
          readToWriter(in, out);
          in.endArray();
          out.endArray();
          break;
        case BEGIN_OBJECT:
          out.beginObject();
          in.beginObject();
          readToWriter(in, out);
          in.endObject();
          out.endObject();
          break;
        case BOOLEAN:
          out.value(in.nextBoolean()); break;
        case NAME:
          out.name(in.nextName()); break;
        case NULL:
          in.nextNull();
          out.nullValue();
          break;
        case NUMBER:
          final String numberString = in.nextString();
          try {
            out.value(Long.parseLong(numberString));
          } catch (NumberFormatException e) {
            out.value(Double.parseDouble(numberString));
          }
          break;
        case STRING:
          out.value(in.nextString()); break;
      }
    }
  }

  public static String readToString(final JsonReader in) throws IOException {
    final StringWriter result =new StringWriter();
    final JsonWriter out = new JsonWriter(result);
    readToWriter(in, out);
    out.close();
    return result.toString();
  }

  public static void writeFromString(final JsonWriter out, String jsonString) throws IOException {
    readToWriter(new JsonReader(new StringReader(jsonString)), out);
  }

}
