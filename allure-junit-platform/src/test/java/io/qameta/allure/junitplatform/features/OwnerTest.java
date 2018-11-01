package io.qameta.allure.junitplatform.features;

import io.qameta.allure.Owner;
import org.junit.jupiter.api.Test;

/**
 * @author charlie (Dmitry Baev).
 */
@Owner("first")
public class OwnerTest {

    @Owner("second")
    @Test
    void secondOwnerTest() {
    }

    @Test
    void defaultOwnerTest() {
    }
}
