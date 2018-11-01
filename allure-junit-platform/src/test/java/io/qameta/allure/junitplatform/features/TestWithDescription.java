package io.qameta.allure.junitplatform.features;

import io.qameta.allure.Description;
import org.junit.jupiter.api.Test;

/**
 * @author a.afrikanov (Andrey Afrikanov).
 */
public class TestWithDescription {

    @Test
    @Description("Test description")
    void testWithDescription() {
    }
}
