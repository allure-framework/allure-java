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

import com.intuit.karate.junit5.Karate;
import org.junit.jupiter.api.Test;

public class KarateTests {

    @Karate.Test
    Karate karateScenarios() {
        return Karate.run();
    }

    @io.karatelabs.junit6.Karate.Test
    io.karatelabs.junit6.Karate currentKarateScenarios() {
        return io.karatelabs.junit6.Karate.run();
    }

    @Test
    void ordinaryJupiterTest() {
    }
}
