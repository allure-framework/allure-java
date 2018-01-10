package io.qameta.allure.junit5.features;

import io.qameta.allure.Description;
import org.junit.jupiter.api.Test;

/**
 * @author a.afrikanov (Andrey Afrikanov).
 */
public class TestsWithDescriptions {

    @Test
    @Description("Test description")
    void testWithDescription() {
    }
}
