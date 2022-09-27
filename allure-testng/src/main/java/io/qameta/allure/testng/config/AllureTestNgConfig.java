package io.qameta.allure.testng.config;

import io.qameta.allure.util.PropertiesUtils;

import java.util.Properties;

public class AllureTestNgConfig {

    public static final String ALLURE_TESTNG_CONFIG = "allure.testng.config.";
    public static final String ALLURE_REPORT_DISABLED_TESTS = ALLURE_TESTNG_CONFIG + "report.disabled.tests";
    private boolean disabledTestsReported;

    public AllureTestNgConfig() {
        final Properties properties = PropertiesUtils.loadAllureProperties();
        disabledTestsReported = Boolean.parseBoolean(properties.getProperty(ALLURE_REPORT_DISABLED_TESTS));
    }

    public boolean isDisabledTestsReported() {
        return disabledTestsReported;
    }

}
