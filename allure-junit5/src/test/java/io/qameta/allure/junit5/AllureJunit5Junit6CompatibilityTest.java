/*
 *  Copyright 2016-2024 Qameta Software Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 */
package io.qameta.allure.junit5;

import io.qameta.allure.Allure;
import io.qameta.allure.Param;
import io.qameta.allure.junitplatform.AllureJunitPlatform;
import io.qameta.allure.model.FixtureResult;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;
import io.qameta.allure.test.AllureResults;
import io.qameta.allure.test.RunUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@Tag("junit6-compat")
@SuppressWarnings("unused")
class AllureJunit5Junit6CompatibilityTest {

    @ExtendWith(AllureJunit5.class)
    static class CompatParametersTest {

        @org.junit.jupiter.params.ParameterizedTest
        @org.junit.jupiter.params.provider.ValueSource(strings = {"a", "b"})
        void paramTest(@Param("id") String value) {
        }
    }

    @Nested
    @ExtendWith(AllureJunit5.class)
    class CompatFixtures {

        @BeforeEach
        void setUp() {
            Allure.step("before step");
        }

        @Test
        void testBody() {
            Allure.step("test step");
        }

        @AfterEach
        void tearDown() {
            Allure.step("after step");
        }
    }

    @Test
    void shouldCaptureParametersWithParamAnnotationOnJunit6() {
        AllureResults results = runWithLauncher(CompatParametersTest.class);

        assertThat(results.getTestResults()).isNotEmpty();

        List<Parameter> allParams = results.getTestResults().stream()
            .flatMap(tr -> tr.getParameters().stream())
            .toList();

        assertThat(allParams)
            .isNotEmpty()
            .extracting(Parameter::getName, Parameter::getValue)
            .contains(
                tuple("id", "a"),
                tuple("id", "b")
            );
    }

    @Test
    void shouldCaptureFixturesAndStepsOnJunit6() {
        AllureResults results = runWithLauncher(CompatFixtures.class);

        assertThat(results.getTestResults()).hasSize(1);
        TestResult testResult = results.getTestResults().get(0);

        assertThat(results.getTestResultContainers())
            .flatExtracting(TestResultContainer::getChildren)
            .contains(testResult.getUuid());

        assertThat(results.getTestResultContainers())
            .flatExtracting(TestResultContainer::getBefores)
            .extracting(FixtureResult::getStatus)
            .contains(Status.PASSED);

        assertThat(results.getTestResultContainers())
            .flatExtracting(TestResultContainer::getAfters)
            .extracting(FixtureResult::getStatus)
            .contains(Status.PASSED);

        assertThat(results.getTestResultContainers())
            .flatExtracting(TestResultContainer::getBefores)
            .flatExtracting(FixtureResult::getSteps)
            .extracting(StepResult::getName)
            .contains("before step");

        assertThat(results.getTestResults())
            .flatExtracting(TestResult::getSteps)
            .extracting(StepResult::getName)
            .contains("test step");

        assertThat(results.getTestResultContainers())
            .flatExtracting(TestResultContainer::getAfters)
            .flatExtracting(FixtureResult::getSteps)
            .extracting(StepResult::getName)
            .contains("after step");
    }

    @io.qameta.allure.Step("Run classes {classes}")
    private AllureResults runWithLauncher(Class<?>... classes) {
        return RunUtils.runTests(lifecycle -> {
            ClassSelector[] selectors = Stream.of(classes)
                .map(DiscoverySelectors::selectClass)
                .toArray(ClassSelector[]::new);

            LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "true")
                .selectors(selectors)
                .build();

            LauncherConfig config = LauncherConfig.builder()
                .enableTestExecutionListenerAutoRegistration(false)
                .addTestExecutionListeners(new AllureJunitPlatform(lifecycle))
                .build();

            Launcher launcher = LauncherFactory.create(config);
            launcher.execute(request);
        });
    }
}
