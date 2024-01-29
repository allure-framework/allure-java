/*
 *  Copyright 2016-2024 Qameta Software Inc
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
 * @author @author charlie (Dmitry Baev baev@qameta.io)
 * @since 1.0-BETA1
 */
@SuppressWarnings({"unused", "PMD.ClassNamingConventions"})
public final class AllureConstants {

    public static final String TEST_RESULT_FILE_SUFFIX = "-result.json";

    public static final String TEST_RESULT_FILE_GLOB = "*-result.json";

    public static final String TEST_RESULT_CONTAINER_FILE_SUFFIX = "-container.json";

    public static final String TEST_RESULT_CONTAINER_FILE_GLOB = "*-container.json";

    public static final String ATTACHMENT_FILE_SUFFIX = "-attachment";

    public static final String ATTACHMENT_FILE_GLOB = "*-attachment*";

    private AllureConstants() {
        throw new IllegalStateException("Do not instance");
    }
}
