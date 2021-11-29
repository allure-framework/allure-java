/*
 *  Copyright 2019 Qameta Software OÜ
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
package io.qameta.allure.junitplatform.features;

import io.qameta.allure.model.Parameter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static io.qameta.allure.junitplatform.AllureJunitPlatform.ALLURE_PARAMETER;
import static io.qameta.allure.junitplatform.AllureJunitPlatform.ALLURE_PARAMETER_EXCLUDED_KEY;
import static io.qameta.allure.junitplatform.AllureJunitPlatform.ALLURE_PARAMETER_MODE_KEY;
import static io.qameta.allure.junitplatform.AllureJunitPlatform.ALLURE_PARAMETER_VALUE_KEY;

/**
 * @author charlie (Dmitry Baev).
 */
public class ReportEntryParameter {

    @Test
    void simpleParameterEvent(TestReporter testReporter) {
        testReporter.publishEntry(buildEvent("some parameter name", "some parameter value"));
    }

    @Test
    void multipleParameterEvent(TestReporter testReporter) {
        testReporter.publishEntry(buildEvent("first name", "first value"));
        testReporter.publishEntry(buildEvent("second name", "second value"));
        testReporter.publishEntry(buildEvent("third name", "third value"));
    }

    @Test
    void modeAndExcluded(TestReporter testReporter) {
        testReporter.publishEntry(buildEvent(
                "hidden excluded",
                "hidden excluded value",
                Parameter.Mode.HIDDEN,
                true
        ));
        testReporter.publishEntry(buildEvent(
                "default excluded",
                "default excluded value",
                Parameter.Mode.DEFAULT,
                true
        ));
        testReporter.publishEntry(buildEvent(
                "masked not excluded",
                "masked not excluded value",
                Parameter.Mode.MASKED,
                false
        ));
    }

    private Map<String, String> buildEvent(final String name,
                                           final String value) {
        return buildEvent(name, value, null, null);
    }

    private Map<String, String> buildEvent(final String name,
                                           final String value,
                                           final Parameter.Mode mode,
                                           final Boolean excluded) {
        final Map<String, String> data = new HashMap<>();
        data.put(ALLURE_PARAMETER, name);
        data.put(ALLURE_PARAMETER_VALUE_KEY, value);
        if (Objects.nonNull(mode)) {
            data.put(ALLURE_PARAMETER_MODE_KEY, mode.name());
        }
        if (Objects.nonNull(excluded)) {
            data.put(ALLURE_PARAMETER_EXCLUDED_KEY, excluded.toString());
        }

        return data;
    }


}
