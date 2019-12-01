package io.qameta.allure.junit5.features;

import io.qameta.allure.Allure;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class BeforeEachFixtureFailureSupport {

    @BeforeEach
    void setUp() {
        Allure.step("setUp 1");
        Allure.step("setUp 2");
        throw new RuntimeException("ta da");
    }

    @Test
    void test1() {
        Allure.step("test1 1");
        Allure.step("test1 2");
    }
}
