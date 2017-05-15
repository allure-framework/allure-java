package io.qameta.allure.testdata;

import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.TestResult;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;

/**
 * @author charlie (Dmitry Baev).
 */
public final class TestData {

    TestData() {
        throw new IllegalStateException("Do not instance");
    }

    public static String randomString() {
        return randomAlphabetic(10);
    }

    public static Label randomLabel() {
        return new Label()
                .withName(randomString())
                .withValue(randomString());
    }

    public static TestResult randomTestResult() {
        return new TestResult()
                .withName(randomString());
    }

    public static Link randomLink() {
        return new Link()
                .withName(randomString())
                .withType(randomString())
                .withUrl(randomString());
    }
}
