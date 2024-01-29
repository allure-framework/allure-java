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
package io.qameta.allure.selenide;

/**
 * Enum wrapper of Selenium {@link org.openqa.selenium.logging.LogType}.

 * @author Yevhen Holiakhovskyi.
 */
public enum LogType {

    /**
     * This log type pertains to logs from the browser.
     */
    BROWSER(org.openqa.selenium.logging.LogType.BROWSER),

    /**
     * This log type pertains to logs from the client.
     */
    CLIENT(org.openqa.selenium.logging.LogType.CLIENT),

    /**
     * This log pertains to logs from the WebDriver implementation.
     */
    DRIVER(org.openqa.selenium.logging.LogType.DRIVER),

    /**
     * This log type pertains to logs relating to performance timings.
     */
    PERFORMANCE(org.openqa.selenium.logging.LogType.PERFORMANCE),

    /**
     * This log type pertains to logs relating to performance timings.
     */
    PROFILER(org.openqa.selenium.logging.LogType.PROFILER),

    /**
     * This log type pertains to logs from the remote server.
     */
    SERVER(org.openqa.selenium.logging.LogType.SERVER);

    private final String logType;

    LogType(final String logType) {
        this.logType = logType;
    }

    @Override
    public String toString() {
        return logType;
    }
}
