package io.qameta.allure;

import io.qameta.allure.model.Label;
import io.qameta.allure.model.TestResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.function.Consumer;

import static io.qameta.allure.testdata.TestData.randomLabel;
import static io.qameta.allure.testdata.TestData.randomLink;
import static io.qameta.allure.testdata.TestData.randomString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author charlie (Dmitry Baev).
 */
public class AllureTest {

    private AllureLifecycle lifecycle;

    @Before
    public void setUp() throws Exception {
        lifecycle = mock(AllureLifecycle.class);
        Allure.setLifecycle(lifecycle);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldAddLabels() throws Exception {
        Label first = randomLabel();
        Label second = randomLabel();
        Label third = randomLabel();

        Allure.addLabels(first, second);

        ArgumentCaptor<Consumer> captor = ArgumentCaptor.forClass(Consumer.class);
        verify(lifecycle, times(1)).updateTestCase(captor.capture());

        Consumer consumer = captor.getValue();
        TestResult result = new TestResult().withLabels(third);
        consumer.accept(result);

        assertThat(result)
                .isNotNull()
                .extracting(TestResult::getLabels)
                .containsExactly(Arrays.asList(third, first, second));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldAddLinks() throws Exception {
        io.qameta.allure.model.Link first = randomLink();
        io.qameta.allure.model.Link second = randomLink();
        io.qameta.allure.model.Link third = randomLink();

        Allure.addLinks(first, second);

        ArgumentCaptor<Consumer> captor = ArgumentCaptor.forClass(Consumer.class);
        verify(lifecycle, times(1)).updateTestCase(captor.capture());

        Consumer consumer = captor.getValue();
        TestResult result = new TestResult().withLinks(third);
        consumer.accept(result);

        assertThat(result)
                .isNotNull()
                .extracting(TestResult::getLinks)
                .containsExactly(Arrays.asList(third, first, second));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldAddDescription() throws Exception {
        String description = randomString();

        Allure.addDescription(description);

        ArgumentCaptor<Consumer> captor = ArgumentCaptor.forClass(Consumer.class);
        verify(lifecycle, times(1)).updateExecutable(captor.capture());

        Consumer consumer = captor.getValue();
        TestResult result = new TestResult().withDescription(randomString());
        consumer.accept(result);

        assertThat(result)
                .isNotNull()
                .hasFieldOrPropertyWithValue("description", description);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldAddDescriptionHtml() throws Exception {
        String description = randomString();

        Allure.addDescriptionHtml(description);

        ArgumentCaptor<Consumer> captor = ArgumentCaptor.forClass(Consumer.class);
        verify(lifecycle, times(1)).updateExecutable(captor.capture());

        Consumer consumer = captor.getValue();
        TestResult result = new TestResult().withDescriptionHtml(randomString());
        consumer.accept(result);

        assertThat(result)
                .isNotNull()
                .hasFieldOrPropertyWithValue("descriptionHtml", description);
    }
}