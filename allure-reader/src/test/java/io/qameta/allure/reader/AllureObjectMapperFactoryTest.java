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
package io.qameta.allure.reader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.qameta.allure.Allure;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.TestResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AllureObjectMapperFactoryTest {

    @Test
    void shouldCreateMapperThatReadsEnumsCaseInsensitively() throws Exception {
        final TestResult result = Allure.step("Deserialize a mixed-case payload with extra fields", () -> {
            final ObjectMapper mapper = AllureObjectMapperFactory.createMapper();
            return mapper.readValue(
                    "{"
                    + "\"name\":\"demo\","
                    + "\"titlePath\":[\"parent\",\"child\"],"
                    + "\"status\":\"pAsSeD\","
                    + "\"stage\":\"fInIsHeD\","
                    + "\"parameters\":[{\"name\":\"secret\",\"value\":\"42\",\"mode\":\"MaSkEd\"}],"
                    + "\"unknown\":\"ignored\""
                    + "}",
                    TestResult.class
            );
        });

        Allure.step("Verify the mapper normalizes enums and keeps the expected payload", () -> {
            assertEquals("demo", result.getName());
            assertEquals("parent", result.getTitlePath().get(0));
            assertEquals("child", result.getTitlePath().get(1));
            assertEquals(Status.PASSED, result.getStatus());
            assertEquals(Stage.FINISHED, result.getStage());
            assertEquals(Parameter.Mode.MASKED, result.getParameters().get(0).getMode());
        });
    }

    @Test
    void deprecatedDeserializersShouldTrimInputAndReturnNullForUnknownValues() throws Exception {
        final ObjectMapper mapper = AllureObjectMapperFactory.createMapper();

        final DeprecatedEnumHolder trimmed = mapper.readValue(
                "{"
                + "\"status\":\" broken \","
                + "\"stage\":\" pending \","
                + "\"mode\":\" hidden \""
                + "}",
                DeprecatedEnumHolder.class
        );
        final DeprecatedEnumHolder unknown = mapper.readValue(
                "{"
                + "\"status\":\"not-a-status\","
                + "\"stage\":\"   \","
                + "\"mode\":\"not-a-mode\""
                + "}",
                DeprecatedEnumHolder.class
        );

        Allure.step("Compare deprecated deserializer output for trimmed and unknown enum values", () -> {
            Allure.addAttachment(
                    "enum-deserialization-summary",
                    "trimmed.status=" + trimmed.status
                    + "\ntrimmed.stage=" + trimmed.stage
                    + "\ntrimmed.mode=" + trimmed.mode
                    + "\nunknown.status=" + unknown.status
                    + "\nunknown.stage=" + unknown.stage
                    + "\nunknown.mode=" + unknown.mode
            );
            assertEquals(Status.BROKEN, trimmed.status);
            assertEquals(Stage.PENDING, trimmed.stage);
            assertEquals(Parameter.Mode.HIDDEN, trimmed.mode);

            assertNull(unknown.status);
            assertNull(unknown.stage);
            assertNull(unknown.mode);
        });
    }

    private static final class DeprecatedEnumHolder {

        @JsonDeserialize(using = StatusDeserializer.class)
        private Status status;

        @JsonDeserialize(using = StageDeserializer.class)
        private Stage stage;

        @JsonDeserialize(using = ParameterModeDeserializer.class)
        private Parameter.Mode mode;
    }
}
