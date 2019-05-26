/*
 *  Copyright 2019 Qameta Software OÜ
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

package io.qameta.allure.selenide;

import org.openqa.selenium.logging.LogType;

/**
 * Enum wrapper of Selenium {@link LogType}.

 * @author Evgeny Golyakhovsky.
 */
public enum LogTypes {

    /**
     * This log type pertains to logs from the browser.
     */
    BROWSER,

    /**
     * This log type pertains to logs from the client.
     */
    CLIENT,

    /**
     * This log pertains to logs from the WebDriver implementation.
     */
    DRIVER,

    /**
     * This log type pertains to logs relating to performance timings.
     */
    PERFORMANCE,

    /**
     * This log type pertains to logs relating to performance timings.
     */
    PROFILER,

    /**
     * This log type pertains to logs from the remote server.
     */
    SERVER;
}
