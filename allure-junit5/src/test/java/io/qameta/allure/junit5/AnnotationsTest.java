package io.qameta.allure.junit5;

import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.junit5.samples.DynamicTests;
import io.qameta.allure.junit5.samples.TestWithClassAnnotations;
import io.qameta.allure.junit5.samples.TestWithMethodAnnotations;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureResultsWriterStub;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * eroshenkoam
 * 08.10.17
 */
public class AnnotationsTest {

    private AllureResultsWriterStub results = new AllureResultsWriterStub();

    private AllureLifecycle lifecycle = new AllureLifecycle(results);

    @Test
    void shouldProcessMethodAnnotations() {
        runClasses(TestWithMethodAnnotations.class);
        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getValue)
                .contains(
                        "epic1", "epic2", "epic3",
                        "feature1", "feature2", "feature3",
                        "story1", "story2", "story3",
                        "some-owner"
                );
    }


    @Test
    void shouldProcessClassAnnotations() {
        runClasses(TestWithClassAnnotations.class);
        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getValue)
                .contains(
                        "epic1", "epic2", "epic3",
                        "feature1", "feature2", "feature3",
                        "story1", "story2", "story3",
                        "some-owner"
                );
    }

    @Test
    @Disabled("not implemented")
    void shouldProcessDynamicTestAnnotations() {
        runClasses(DynamicTests.class);
        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(3)
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getValue)
                .contains(
                        "epic1", "epic2", "epic3",
                        "feature1", "feature2", "feature3",
                        "story1", "story2", "story3",
                        "some-owner"
                );
    }

    private void runClasses(Class<?>... classes) {
        final ClassSelector[] classSelectors = Stream.of(classes)
                .map(DiscoverySelectors::selectClass)
                .toArray(ClassSelector[]::new);
        final LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(classSelectors)
                .build();

        AllureJunit5AnnotationProcessor.setLifecycle(lifecycle);
        final Launcher launcher = LauncherFactory.create();

        System.setProperty("junit.jupiter.extensions.autodetection.enabled", "true");
        launcher.execute(request, new AllureJunit5(lifecycle));
    }

}
