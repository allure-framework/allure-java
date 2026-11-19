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
package io.qameta.allure.cucumber7jvm;

import java.net.URI;
import java.nio.file.Paths;

/**
 * Shared scenario identities for execution and reporting.
 */
final class ScenarioUtils {

    private static final String WORKING_DIRECTORY = Paths.get("").toUri().getSchemeSpecificPart();

    private ScenarioUtils() {
    }

    static String scenarioUri(final URI uri) {
        final String path = uri.getSchemeSpecificPart();
        return path.startsWith(WORKING_DIRECTORY) ? path.substring(WORKING_DIRECTORY.length()) : path;
    }
}
