/*
 *  Copyright 2016-2026 Qameta Software Inc
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
package io.qameta.allure.test;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureConstants;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Locale;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static com.fasterxml.jackson.databind.MapperFeature.USE_WRAPPER_NAME_AS_PROPERTY_NAME;

/**
 * @author charlie (Dmitry Baev).
 */
public final class AllureTestCommonsUtils {

    private static final ObjectWriter WRITER = JsonMapper
            .builder()
            .configure(USE_WRAPPER_NAME_AS_PROPERTY_NAME, true)
            .serializationInclusion(NON_DEFAULT)
            .build()
            .registerModule(new SimpleModule()
                    .addSerializer(Status.class, new StatusSerializer())
                    .addSerializer(Stage.class, new StageSerializer())
                    .addSerializer(Parameter.Mode.class, new ParameterModeSerializer())
            )
            .writerWithDefaultPrettyPrinter();

    private AllureTestCommonsUtils() {
        throw new IllegalStateException("do not instance");
    }

    /**
     * Attach {@link AllureResults} to the report.
     */
    public static void attach(final AllureResults allureResults) {
        allureResults.getTestResults().forEach(testResult -> {
            try {
                Allure.addAttachment(
                        testResult.getUuid() + AllureConstants.TEST_RESULT_FILE_SUFFIX,
                        WRITER.writeValueAsString(testResult)
                );
            } catch (JsonProcessingException e) {
                throw new UncheckedIOException(e);
            }
        });

        allureResults.getTestResultContainers().forEach(container -> {
            try {
                Allure.addAttachment(
                        container.getUuid() + AllureConstants.TEST_RESULT_CONTAINER_FILE_SUFFIX,
                        WRITER.writeValueAsString(container)
                );
            } catch (JsonProcessingException e) {
                throw new UncheckedIOException(e);
            }
        });

        allureResults.getAttachments().forEach((fileName, body) -> Allure
                .addAttachment(
                        fileName,
                        new ByteArrayInputStream(body)
                )
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
