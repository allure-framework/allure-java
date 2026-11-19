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

public final class Main {
    public static void main(final String[] args) {
        final var runner = new org.junit.runner.JUnitCore();
        runner.addListener(new io.qameta.allure.junit4.AllureJunit4());
        final var result = runner.run(
                org.junit.runner.Request.aClass(fixture.tests.SampleTest.class)
                        .filterWith(new io.qameta.allure.junit4.AllureJunit4Filter())
        );
        if (!result.wasSuccessful() || result.getRunCount() != 0) {
            throw new AssertionError(result.getFailures());
        }
    }
}
