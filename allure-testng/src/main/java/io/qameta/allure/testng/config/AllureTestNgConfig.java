package io.qameta.allure.testng.config;

import io.qameta.allure.util.PropertiesUtils;

import java.util.Properties;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.setProperty;

public class AllureTestNgConfig {

    public static final String ALLURE_TESTNG_HIDE_DISABLED_TESTS = "allure.testng.hide.disabled.tests";
    private static boolean hideDisabledTests;

    public boolean isHideDisabledTests() {
        return hideDisabledTests;
    }

    public AllureTestNgConfig setConfiguration(final String property, final String value) {
        setProperty(property, value);
        return this;
    }

    public static AllureTestNgConfig loadConfigProperties() {
        final Properties properties = PropertiesUtils.loadAllureProperties();
        hideDisabledTests = !parseBoolean(properties.getProperty(ALLURE_TESTNG_HIDE_DISABLED_TESTS));
        return new AllureTestNgConfig();
    }

}
