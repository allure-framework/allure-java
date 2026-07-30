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
package io.qameta.allure.karate.features;

import io.karatelabs.core.Runner;
import io.karatelabs.junit6.Karate;
import io.qameta.allure.karate.AllureKarate;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

/**
 * Real Karate JUnit 6 fixture launched programmatically by the integration smoke test.
 */
public class KarateJunit6Tests {

    @Karate.Test
    Iterable<DynamicNode> karateScenarios() {
        final Karate karate = Karate.run("classpath:testdata/junit6-smoke.feature")
                .outputHtmlReport(false)
                .outputJunitXml(false)
                .outputCucumberJson(false);
        getDelegate(karate).listener(new AllureKarate());
        return karate;
    }

    @Test
    void ordinaryJupiterTest() {
    }

    private static Runner.Builder getDelegate(final Karate karate) {
        try {
            final Field field = Karate.class.getDeclaredField("delegate");
            field.setAccessible(true);
            return (Runner.Builder) field.get(karate);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not register the Allure listener with Karate JUnit 6", e);
        }
    }
}
