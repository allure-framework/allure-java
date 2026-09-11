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

import fixture.spi.Greeting;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureExternalKey;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.listener.ContainerLifecycleListener;
import io.qameta.allure.listener.FixtureLifecycleListener;
import io.qameta.allure.listener.StepLifecycleListener;
import io.qameta.allure.listener.TestLifecycleListener;
import io.qameta.allure.model.FixtureResult;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;
import io.qameta.allure.util.ServiceLoaderUtils;

import java.util.List;

public class Main
        implements
            TestLifecycleListener,
            ContainerLifecycleListener,
            FixtureLifecycleListener,
            StepLifecycleListener,
            Greeting {

    public static void main(final String[] args) {
        final AllureLifecycle lifecycle = Allure.getLifecycle();
        final AllureExternalKey scope = AllureExternalKey.of(Main.class, "scope");
        final AllureExternalKey fixture = AllureExternalKey.of(Main.class, "fixture");
        final AllureExternalKey test = AllureExternalKey.of(Main.class, "test");
        lifecycle.registerScope(scope);
        lifecycle.startBeforeFixture(scope, fixture, new FixtureResult().setName("setup"));
        lifecycle.stopFixture(fixture);
        lifecycle.scheduleTest(List.of(scope), test, new TestResult().setUuid("test").setName("core"));
        lifecycle.startTest(test);
        Allure.label(
                "custom service", ServiceLoaderUtils.load(Greeting.class, Main.class.getClassLoader())
                        .get(0).value()
        );
        Allure.step("operation");
        Allure.attachment("payload", "text/plain", "module payload");
        lifecycle.updateTest(result -> result.setStatus(Status.PASSED));
        lifecycle.stopTest(test);
        lifecycle.writeTest(test);
        lifecycle.writeScope(scope);
    }

    @Override
    public String value() {
        return "loaded";
    }

    @Override
    public void beforeTestWrite(final TestResult result) {
        result.getLabels().add(new Label().setName("listener").setValue("test"));
    }

    @Override
    public void beforeContainerWrite(final TestResultContainer result) {
        result.setName("container listener");
    }

    @Override
    public void beforeFixtureStop(final FixtureResult result) {
        result.setDescription("fixture listener");
    }

    @Override
    public void beforeStepStart(final StepResult result) {
        result.setName("step listener: " + result.getName());
    }
}
