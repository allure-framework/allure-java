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
package io.qameta.allure.junitplatform.features;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author charlie (Dmitry Baev).
 */
@Epic("Parent epic")
public class NestedTests {

    @Feature("Parent feature")
    @Test
    void parentTest() {
    }

    @Feature("Feature 1")
    @Nested
    class Feature1 {

        @Test
        void feature1Test() {
        }
    }

    @Feature("Feature 2")
    @Nested
    class Feature2 {

        @Test
        void feature2Test() {
        }

        @Story("Story 1")
        @Nested
        class Story1 {

            @Test
            void story1Test() {
            }

        }
    }
}
