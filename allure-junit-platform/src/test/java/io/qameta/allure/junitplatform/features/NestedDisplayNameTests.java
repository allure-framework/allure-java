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
package io.qameta.allure.junitplatform.features;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("A customer object")
public class NestedDisplayNameTests {

    @Test
    @DisplayName("can be created with the dao")
    void canBeCreated() {
    }

    @Nested
    @DisplayName("when created")
    class WhenCreated {

        @Test
        @DisplayName("it must be saved to the dao")
        void mustBeSaved() {
        }

        @Nested
        @DisplayName("after saving a customer")
        class AfterSaving {

            @Test
            @DisplayName("it can be fetched from the dao")
            void canBeFetched() {
            }

            @Test
            @DisplayName("it cannot be deleted by wrong id")
            void cannotBeDeletedByWrongId() {
            }

            @Nested
            @DisplayName("and reloading the dao")
            class AfterReload {

                @Test
                @DisplayName("it is still present")
                void isStillPresent() {
                }
            }
        }
    }
}
