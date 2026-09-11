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

import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

public final class Main {

    private Main() {
    }

    public static void main(final String[] args) {
        final SummaryGeneratingListener summary = new SummaryGeneratingListener();
        final var launcher = LauncherFactory.create();
        launcher.registerTestExecutionListeners(summary);
        launcher.execute(
                LauncherDiscoveryRequestBuilder.request()
                        .selectors(DiscoverySelectors.selectClass("fixture.tests.SampleTest"))
                        .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "true")
                        .build()
        );
        if (summary.getSummary().getTestsSucceededCount() != 1) {
            throw new IllegalStateException(
                    "Expected one successful Spock feature: "
                            + summary.getSummary().getFailures()
            );
        }
    }
}
