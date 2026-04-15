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

import io.qameta.allure.Allure;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestUtilitiesTest {

    @Test
    void shouldGenerateStableThreadLocalRandomPerThread() throws Exception {
        final EnhancedRandom mainThread = ThreadLocalEnhancedRandom.current();
        final AtomicReference<EnhancedRandom> workerThread = new AtomicReference<>();
        final Thread thread = new Thread(() ->
                workerThread.set(ThreadLocalEnhancedRandom.current())
        );

        Allure.step("Resolve thread-local random generators on two threads and compare their identities", () -> {
            thread.start();
            thread.join();
            Allure.addAttachment(
                    "thread-local-random-identities",
                    "main=" + System.identityHashCode(mainThread)
                    + "\nworker=" + System.identityHashCode(workerThread.get())
            );
            assertSame(mainThread, ThreadLocalEnhancedRandom.current());
            assertNotSame(mainThread, workerThread.get());
        });
    }

    @Test
    void shouldGenerateExpectedRandomTestDataShapes() {
        final String name = TestData.randomName();
        final String id = TestData.randomId();
        final String value = TestData.randomString(16);

        assertEquals(10, name.length());
        assertEquals(10, id.length());
        assertEquals(16, value.length());
        assertTrue(name.matches("[A-Za-z]+"));
        assertTrue(id.matches("[A-Za-z0-9]+"));
        assertTrue(value.matches("[A-Za-z0-9]+"));
    }
}
