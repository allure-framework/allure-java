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
package io.qameta.allure;

/**
 * Defines common file naming constants for Allure result artifacts.
 *
 * <p>Use these constants when reading or writing result JSON files, container JSON files, and attachment files so custom tooling follows the same naming conventions as the Allure lifecycle.</p>
 */
@SuppressWarnings({"unused"})
public final class AllureConstants {

    /**
     * File suffix used for test result artifacts.
     */
    public static final String TEST_RESULT_FILE_SUFFIX = "-result.json";

    /**
     * Glob pattern used to find test result artifacts.
     */
    public static final String TEST_RESULT_FILE_GLOB = "*-result.json";

    /**
     * File suffix used for test result container artifacts.
     */
    public static final String TEST_RESULT_CONTAINER_FILE_SUFFIX = "-container.json";

    /**
     * Glob pattern used to find test result container artifacts.
     */
    public static final String TEST_RESULT_CONTAINER_FILE_GLOB = "*-container.json";

    /**
     * File suffix used for attachment artifacts.
     */
    public static final String ATTACHMENT_FILE_SUFFIX = "-attachment";

    /**
     * Glob pattern used to find attachment artifacts.
     */
    public static final String ATTACHMENT_FILE_GLOB = "*-attachment*";

    private AllureConstants() {
        throw new IllegalStateException("Do not instance");
    }
}
