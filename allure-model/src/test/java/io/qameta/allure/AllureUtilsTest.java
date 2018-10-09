package io.qameta.allure;

import org.junit.Test;

import static io.qameta.allure.AllureUtils.generateTestResultContainerName;
import static io.qameta.allure.AllureUtils.generateTestResultName;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author charlie (Dmitry Baev).
 */
public class AllureUtilsTest {

    @Test
    public void shouldGenerateTestResultName() {
        String name = generateTestResultName();
        assertThat(name)
                .isNotNull()
                .matches(".+-result\\.json");
    }

    @Test
    public void shouldGenerateTestResultContainerName() {
        String name = generateTestResultContainerName();
        assertThat(name)
                .isNotNull()
                .matches(".+-container\\.json");
    }
}