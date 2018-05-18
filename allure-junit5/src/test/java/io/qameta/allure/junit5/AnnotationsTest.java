package io.qameta.allure.junit5;

import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.junit5.samples.DynamicTests;
import io.qameta.allure.junit5.samples.TestWithClassLabels;
import io.qameta.allure.junit5.samples.TestWithClassLinks;
import io.qameta.allure.junit5.samples.TestWithMethodLabels;
import io.qameta.allure.junit5.samples.TestWithMethodLinks;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureResultsWriterStub;
import org.junit.jupiter.api.BeforeEach;
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

public class AnnotationsTest {

    private AllureResultsWriterStub results = new AllureResultsWriterStub();

    private AllureLifecycle lifecycle = new AllureLifecycle(results);

    @BeforeEach
    void setProperties() {
        System.setProperty("junit.jupiter.extensions.autodetection.enabled", "true");
    }

    @Test
    void shouldProcessMethodLabels() {
        runClasses(TestWithMethodLabels.class);
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
    void shouldProcessClassLabels() {
        runClasses(TestWithClassLabels.class);
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
    void shouldProcessMethodLinks() {
        runClasses(TestWithMethodLinks.class);
        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .flatExtracting(TestResult::getLinks)
                .extracting(Link::getName)
                .contains(
                        "LINK-1", "LINK-2", "LINK-3",
                        "ISSUE-1", "ISSUE-2", "ISSUE-3",
                        "TMS-1", "TMS-2", "TMS-3"
                );
    }

    @Test
    void shouldProcessClassLinks() {
        runClasses(TestWithClassLinks.class);
        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .flatExtracting(TestResult::getLinks)
                .extracting(Link::getName)
                .contains(
                        "LINK-1", "LINK-2", "LINK-3",
                        "ISSUE-1", "ISSUE-2", "ISSUE-3",
                        "TMS-1", "TMS-2", "TMS-3"
                );
    }

    @Test
    @Disabled("not implemented")
    void shouldProcessDynamicTestLabels() {
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

    @BeforeEach
    void clearProperties() {
        System.setProperty("junit.jupiter.extensions.autodetection.enabled", "false");
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
        launcher.execute(request, new AllureJunit5(lifecycle));
    }

}
