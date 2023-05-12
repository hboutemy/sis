/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.internal.coveragejson.binding;

import jakarta.json.bind.annotation.JsonbTypeDeserializer;
import jakarta.json.bind.annotation.JsonbTypeSerializer;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import java.lang.reflect.Type;
import java.util.Map.Entry;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
@JsonbTypeDeserializer(Ranges.Deserializer.class)
@JsonbTypeSerializer(Ranges.Serializer.class)
public final class Ranges extends Dictionary<NdArray> {

    public static class Deserializer implements JsonbDeserializer<Ranges> {
        @Override
        public Ranges deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
            final Ranges candidate = new Ranges();
            while (parser.hasNext()) {
                final JsonParser.Event event = parser.next();
                if (event == JsonParser.Event.KEY_NAME) {
                    // Deserialize inner object
                    final String name = parser.getString();
                    final NdArray value = ctx.deserialize(NdArray.class, parser);
                    candidate.setAnyProperty(name, value);
                }
            }
            return candidate;
        }
    }

    public static class Serializer implements JsonbSerializer<Ranges> {

        @Override
        public void serialize(Ranges ranges, JsonGenerator jg, SerializationContext sc) {
            jg.writeStartObject();
            for (Entry<String,NdArray> entry : ranges.any.entrySet()) {
                sc.serialize(entry.getKey(), entry.getValue(), jg);
            }
            jg.writeEnd();
        }

    }
}
