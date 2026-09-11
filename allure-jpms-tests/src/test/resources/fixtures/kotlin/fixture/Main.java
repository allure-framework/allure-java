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

import io.qameta.allure.AllureExternalKey;
import io.qameta.allure.AllureThreadBinding;
import io.qameta.allure.kotlin.coroutines.AllureCoroutines;
import io.qameta.allure.kotlin.extensions.AllureKotlin;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.TestResult;
import kotlin.Unit;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.ThreadContextElement;

public final class Main {
    public static void main(final String[] args) throws Exception {
        final var lifecycle = AllureKotlin.getLifecycle();
        final AllureExternalKey test = AllureExternalKey.of(Main.class, "kotlin");
        lifecycle.scheduleTest(test, new TestResult().setName("Kotlin modules"));
        lifecycle.startTest(test);
        final var context = AllureCoroutines.allureContext();
        final var worker = new Thread(() -> {
            @SuppressWarnings("unchecked")
            final var element = (ThreadContextElement<AllureThreadBinding>) context;
            final var previous = element.updateThreadContext(EmptyCoroutineContext.INSTANCE);
            try {
                AllureKotlin.step("Kotlin operation", scope -> {
                    AllureKotlin.attachment("payload", "Kotlin module payload", "text/plain", null);
                    return Unit.INSTANCE;
                });
            } finally {
                element.restoreThreadContext(EmptyCoroutineContext.INSTANCE, previous);
            }
        });
        worker.start();
        worker.join();
        lifecycle.updateTest(result -> result.setStatus(Status.PASSED));
        lifecycle.stopTest(test);
        lifecycle.writeTest(test);
    }
}
