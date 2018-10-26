package io.qameta.allure;

import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.TestResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.function.Consumer;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author charlie (Dmitry Baev).
 */
class AllureTest {

    private AllureLifecycle lifecycle;

    @BeforeEach
    void setUp() {
        lifecycle = mock(AllureLifecycle.class);
        Allure.setLifecycle(lifecycle);
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldAddLabels() {
        Label first = random(Label.class);
        Label second = random(Label.class);
        Label third = random(Label.class);

        Allure.addLabels(first, second);

        ArgumentCaptor<Consumer> captor = ArgumentCaptor.forClass(Consumer.class);
        verify(lifecycle, times(1)).updateTestCase(captor.capture());

        Consumer consumer = captor.getValue();
        TestResult result = new TestResult().setLabels(third);
        consumer.accept(result);

        assertThat(result)
                .isNotNull()
                .extracting(TestResult::getLabels)
                .containsExactly(Arrays.asList(third, first, second));
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldAddLinks() {
        io.qameta.allure.model.Link first = random(Link.class);
        io.qameta.allure.model.Link second = random(Link.class);
        io.qameta.allure.model.Link third = random(Link.class);

        Allure.addLinks(first, second);

        ArgumentCaptor<Consumer> captor = ArgumentCaptor.forClass(Consumer.class);
        verify(lifecycle, times(1)).updateTestCase(captor.capture());

        Consumer consumer = captor.getValue();
        TestResult result = new TestResult().setLinks(third);
        consumer.accept(result);

        assertThat(result)
                .isNotNull()
                .extracting(TestResult::getLinks)
                .containsExactly(Arrays.asList(third, first, second));
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldAddDescription() {
        String description = random(String.class);

        Allure.addDescription(description);

        ArgumentCaptor<Consumer> captor = ArgumentCaptor.forClass(Consumer.class);
        verify(lifecycle, times(1)).updateTestCase(captor.capture());

        Consumer consumer = captor.getValue();
        TestResult result = new TestResult().setDescription(random(String.class));
        consumer.accept(result);

        assertThat(result)
                .isNotNull()
                .hasFieldOrPropertyWithValue("description", description);
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldAddDescriptionHtml() {
        String description = random(String.class);

        Allure.addDescriptionHtml(description);

        ArgumentCaptor<Consumer> captor = ArgumentCaptor.forClass(Consumer.class);
        verify(lifecycle, times(1)).updateTestCase(captor.capture());

        Consumer consumer = captor.getValue();
        TestResult result = new TestResult().setDescriptionHtml(random(String.class));
        consumer.accept(result);

        assertThat(result)
                .isNotNull()
                .hasFieldOrPropertyWithValue("descriptionHtml", description);
    }
}