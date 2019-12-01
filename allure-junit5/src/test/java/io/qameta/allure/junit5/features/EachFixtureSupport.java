package io.qameta.allure.junit5.features;

import io.qameta.allure.Allure;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class EachFixtureSupport {

    @BeforeEach
    void setUp() {
        Allure.step("setUp 1");
        Allure.step("setUp 2");
    }

    @Test
    void test1() {
        Allure.step("test1 1");
        Allure.step("test1 2");
    }

    @AfterEach
    void tearDown() {
        Allure.step("tearDown 1");
        Allure.step("tearDown 2");
    }
}
