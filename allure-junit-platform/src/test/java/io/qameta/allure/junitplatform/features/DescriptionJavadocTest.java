package io.qameta.allure.junitplatform.features;

import io.qameta.allure.Description;
import org.junit.jupiter.api.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class DescriptionJavadocTest {

    /**
     * Test javadoc description.
     */
    @Description(useJavaDoc = true)
    @Test
    void testWithJavadocDescription() {
    }
}
