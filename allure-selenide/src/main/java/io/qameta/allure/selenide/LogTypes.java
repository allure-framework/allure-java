package io.qameta.allure.selenide;

import org.openqa.selenium.logging.LogType;

/**
 * Enum wrapper of Selenium {@link LogType}.
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
