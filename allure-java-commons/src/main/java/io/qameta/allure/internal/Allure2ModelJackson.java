/*
 *  Copyright 2016-2024 Qameta Software Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.qameta.allure.internal;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;

import java.io.IOException;
import java.util.Locale;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.databind.MapperFeature.USE_WRAPPER_NAME_AS_PROPERTY_NAME;
import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;

/**
 * The type Allure 2 model jackson.
 *
 * @author baev (Dmitry Baev).
 */
public final class Allure2ModelJackson {

    public static final String INDENT_OUTPUT_PROPERTY_NAME = "allure.results.indentOutput";

    private Allure2ModelJackson() {
        throw new IllegalStateException("Do not instance Allure2ModelJackson");
    }

    public static ObjectMapper createMapper() {
        return JsonMapper
                .builder()
                .configure(USE_WRAPPER_NAME_AS_PROPERTY_NAME, true)
                .serializationInclusion(NON_NULL)
                .configure(INDENT_OUTPUT, Boolean.getBoolean(INDENT_OUTPUT_PROPERTY_NAME))
                .build()
                .registerModule(new SimpleModule()
                        .addSerializer(Status.class, new StatusSerializer())
                        .addSerializer(Stage.class, new StageSerializer())
                        .addSerializer(Parameter.Mode.class, new ParameterModeSerializer())
                );
    }

    /**
     * Parameter mode serializer.
     */
    private static class ParameterModeSerializer extends StdSerializer<Parameter.Mode> {
        protected ParameterModeSerializer() {
            super(Parameter.Mode.class);
        }

        @Override
        public void serialize(final Parameter.Mode value,
                              final JsonGenerator gen,
                              final SerializerProvider provider) throws IOException {
            gen.writeString(value.name().toLowerCase(Locale.ENGLISH));
        }
    }

    /**
     * Stage serializer.
     */
    private static class StageSerializer extends StdSerializer<Stage> {
        protected StageSerializer() {
            super(Stage.class);
        }

        @Override
        public void serialize(final Stage value,
                              final JsonGenerator gen,
                              final SerializerProvider provider) throws IOException {
            gen.writeString(value.name().toLowerCase(Locale.ENGLISH));
        }
    }

    /**
     * Status serializer.
     */
    private static class StatusSerializer extends StdSerializer<Status> {
        protected StatusSerializer() {
            super(Status.class);
        }

        @Override
        public void serialize(final Status value,
                              final JsonGenerator gen,
                              final SerializerProvider provider) throws IOException {
            gen.writeString(value.name().toLowerCase(Locale.ENGLISH));
        }
    }
}
