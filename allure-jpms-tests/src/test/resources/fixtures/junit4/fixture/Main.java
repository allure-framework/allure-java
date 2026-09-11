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
package fixture;

import io.qameta.allure.junit4.AllureJunit4;
import org.junit.runner.JUnitCore;

public final class Main {
    public static void main(final String[] args) {
        final JUnitCore runner = new JUnitCore();
        runner.addListener(new AllureJunit4());
        final var result = runner.run(fixture.tests.SampleTest.class);
        if (result.getRunCount() != 1 || !result.wasSuccessful()) {
            throw new AssertionError("Expected one successful JUnit 4 test: " + result.getFailures());
        }
    }
}
