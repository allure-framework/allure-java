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
package io.qameta.allure.junitplatform;

import io.qameta.allure.Step;
import io.qameta.allure.test.AllureResults;
import io.qameta.allure.test.RunUtils;
import io.qameta.allure.testfilter.TestPlan;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import java.util.stream.Stream;

/**
 * @author charlie (Dmitry Baev).
 */
public final class AllureJunitPlatformTestUtils {

    private AllureJunitPlatformTestUtils() {
        throw new IllegalStateException("do not instance");
    }

    @Step("Run classes {classes}")
    public static AllureResults runClasses(final Class<?>... classes) {
        return runClasses(null, classes);
    }

    @Step("Run classes {classes}")
    public static AllureResults runClasses(final TestPlan testPlan, final Class<?>... classes) {
        return RunUtils.runTests(lifecycle -> {
            final ClassSelector[] classSelectors = Stream.of(classes)
                    .map(DiscoverySelectors::selectClass)
                    .toArray(ClassSelector[]::new);

            final LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                    .filters(new AllurePostDiscoveryFilter(testPlan))
                    .selectors(classSelectors)
                    .build();

            final LauncherConfig config = LauncherConfig.builder()
                    .enableTestExecutionListenerAutoRegistration(false)
                    .addTestExecutionListeners(new AllureJunitPlatform(lifecycle))
                    .enablePostDiscoveryFilterAutoRegistration(false)
                    .build();
            final Launcher launcher = LauncherFactory.create(config);
            launcher.execute(request);
        });
    }

}
