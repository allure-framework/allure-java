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

import io.qameta.allure.Allure;
import io.qameta.allure.AllureExternalKey;
import io.qameta.allure.jsonunit.JsonPatchMatcher;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.TestResult;
import org.hamcrest.StringDescription;

public final class Main {
    public static void main(final String[] args) {
        final var lifecycle = Allure.getLifecycle();
        final AllureExternalKey test = AllureExternalKey.of(Main.class, "jsonunit");
        lifecycle.scheduleTest(test, new TestResult().setName("JSON module"));
        lifecycle.startTest(test);
        final var matcher = JsonPatchMatcher.jsonEquals("{\"value\":1}");
        if (matcher.matches("{\"value\":2}")) {
            throw new AssertionError("Expected a JSON difference");
        }
        matcher.describeMismatch("{\"value\":2}", new StringDescription());
        lifecycle.updateTest(result -> result.setStatus(Status.PASSED));
        lifecycle.stopTest(test);
        lifecycle.writeTest(test);
    }
}
