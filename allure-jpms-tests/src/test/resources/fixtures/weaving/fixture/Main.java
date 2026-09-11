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
import io.qameta.allure.Attachment;
import io.qameta.allure.Step;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.TestResult;

public final class Main {

    private Main() {
    }

    public static void main(final String[] args) {
        final AllureExternalKey test = AllureExternalKey.of(Main.class, "test");
        Allure.getLifecycle().scheduleTest(test, new TestResult().setName("woven test"));
        Allure.getLifecycle().startTest(test);
        operation(new User());
        Allure.getLifecycle().updateTest(result -> result.setStatus(Status.PASSED));
        Allure.getLifecycle().stopTest(test);
        Allure.getLifecycle().writeTest(test);
    }

    @Step("operation for {user.name}")
    private static void operation(final User user) {
        payload();
    }

    @Attachment(
            value = "payload",
            type = "text/plain"
    )
    private static String payload() {
        return "woven payload";
    }

    private static final class User {
        private final String name = "module user";
    }
}
