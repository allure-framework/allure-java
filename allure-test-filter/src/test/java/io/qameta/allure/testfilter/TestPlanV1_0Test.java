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
package io.qameta.allure.testfilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Allure;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestPlanV1_0Test {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void shouldMatchByAllureIdOrSelector() {
        final TestPlanV1_0 plan = new TestPlanV1_0().setTests(List.of(
                new TestPlanV1_0.TestCase().setId("A-1"),
                new TestPlanV1_0.TestCase().setSelector("pkg.Test#name")
        ));

        assertTrue(plan.isSelected("A-1", "other.Test#name"));
        assertTrue(plan.isSelected("other-id", "pkg.Test#name"));
        assertFalse(plan.isSelected("other-id", "other.Test#name"));
    }

    @Test
    void shouldDeserializeVersionedPlans() throws Exception {
        final TestPlan plan = OBJECT_MAPPER.readValue(
                "{"
                + "\"version\":\"1.0\","
                + "\"tests\":[{\"id\":\"A-1\",\"selector\":\"pkg.Test#name\"}]"
                + "}",
                TestPlan.class
        );

        assertInstanceOf(TestPlanV1_0.class, plan);
        assertTrue(((TestPlanV1_0) plan).isSelected("A-1", "pkg.Test#name"));
    }

    @Test
    void shouldFallbackToUnknownPlanForUnknownVersion() throws Exception {
        final TestPlan plan = Allure.step("Deserialize a plan with an unsupported version", () ->
                OBJECT_MAPPER.readValue(
                        "{\"version\":\"2.0\"}",
                        TestPlan.class
                )
        );

        Allure.step("Verify the parser falls back to the unknown plan representation", () ->
                assertInstanceOf(TestPlanUnknown.class, plan)
        );
    }
}
