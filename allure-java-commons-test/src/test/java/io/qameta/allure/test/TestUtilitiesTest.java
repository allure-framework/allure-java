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

import io.github.benas.randombeans.api.EnhancedRandom;
import io.qameta.allure.Allure;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TestUtilitiesTest {

    @Test
    void shouldGenerateStableThreadLocalRandomPerThread() throws Exception {
        final EnhancedRandom mainThread = ThreadLocalEnhancedRandom.current();
        final AtomicReference<EnhancedRandom> workerThread = new AtomicReference<>();
        final Thread thread = new Thread(
                () -> workerThread.set(ThreadLocalEnhancedRandom.current())
        );

        Allure.step("Resolve thread-local random generators on two threads and compare their identities", () -> {
            thread.start();
            thread.join();
            Allure.addAttachment(
                    "thread-local-random-identities",
                    "main=" + System.identityHashCode(mainThread)
                            + "\nworker=" + System.identityHashCode(workerThread.get())
            );
            assertThat(ThreadLocalEnhancedRandom.current())
                    .isSameAs(mainThread);
            assertThat(workerThread.get())
                    .isNotSameAs(mainThread);
        });
    }

    @Test
    void shouldGenerateExpectedRandomTestDataShapes() {
        final RandomValues values = Allure.step(
                "Generate random test data values",
                () -> new RandomValues(TestData.randomName(), TestData.randomId(), TestData.randomString(16))
        );

        Allure.step("Verify generated values match expected shapes", () -> {
            assertThat(values.name)
                    .hasSize(10)
                    .matches("[A-Za-z]+");
            assertThat(values.id)
                    .hasSize(10)
                    .matches("[A-Za-z0-9]+");
            assertThat(values.value)
                    .hasSize(16)
                    .matches("[A-Za-z0-9]+");
        });
    }

    private record RandomValues(String name, String id, String value) {
    }
}
