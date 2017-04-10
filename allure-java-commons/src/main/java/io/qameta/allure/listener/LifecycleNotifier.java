package io.qameta.allure.listener;

import io.qameta.allure.model.FixtureResult;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;

import java.util.List;

/**
 * @since 2.0
 */
@SuppressWarnings("PMD.TooManyMethods")
public class LifecycleNotifier implements ContainerLifecycleListener,
        TestLifecycleListener, FixtureLifecycleListener, StepLifecycleListener {

    private final List<ContainerLifecycleListener> containerListeners;

    private final List<TestLifecycleListener> testListeners;

    private final List<FixtureLifecycleListener> fixtureListeners;

    private final List<StepLifecycleListener> stepListeners;

    public LifecycleNotifier(final List<ContainerLifecycleListener> containerListeners,
                             final List<TestLifecycleListener> testListeners,
                             final List<FixtureLifecycleListener> fixtureListeners,
                             final List<StepLifecycleListener> stepListeners) {
        this.containerListeners = containerListeners;
        this.testListeners = testListeners;
        this.fixtureListeners = fixtureListeners;
        this.stepListeners = stepListeners;
    }


    @Override
    public void beforeTestSchedule(final TestResult result) {
        testListeners.forEach(listener -> listener.beforeTestSchedule(result));
    }

    @Override
    public void afterTestSchedule(final TestResult result) {
        testListeners.forEach(listener -> listener.afterTestSchedule(result));
    }

    @Override
    public void beforeTestUpdate(final TestResult result) {
        testListeners.forEach(listener -> listener.beforeTestUpdate(result));
    }

    @Override
    public void afterTestUpdate(final TestResult result) {
        testListeners.forEach(listener -> listener.afterTestUpdate(result));
    }

    @Override
    public void beforeTestStart(final TestResult result) {
        testListeners.forEach(listener -> listener.beforeTestStart(result));
    }

    @Override
    public void afterTestStart(final TestResult result) {
        testListeners.forEach(listener -> listener.afterTestStart(result));
    }

    @Override
    public void beforeTestStop(final TestResult result) {
        testListeners.forEach(listener -> listener.beforeTestStop(result));
    }

    @Override
    public void afterTestStop(final TestResult result) {
        testListeners.forEach(listener -> listener.afterTestStop(result));
    }

    @Override
    public void beforeTestWrite(final TestResult result) {
        testListeners.forEach(listener -> listener.beforeTestWrite(result));
    }

    @Override
    public void afterTestWrite(final TestResult result) {
        testListeners.forEach(listener -> listener.afterTestWrite(result));
    }

    @Override
    public void beforeContainerStart(final TestResultContainer container) {
        containerListeners.forEach(listener -> listener.beforeContainerStart(container));
    }

    @Override
    public void afterContainerStart(final TestResultContainer container) {
        containerListeners.forEach(listener -> listener.afterContainerStart(container));
    }

    @Override
    public void beforeContainerUpdate(final TestResultContainer container) {
        containerListeners.forEach(listener -> listener.beforeContainerUpdate(container));
    }

    @Override
    public void afterContainerUpdate(final TestResultContainer container) {
        containerListeners.forEach(listener -> listener.afterContainerUpdate(container));
    }

    @Override
    public void beforeContainerStop(final TestResultContainer container) {
        containerListeners.forEach(listener -> listener.beforeContainerStop(container));
    }

    @Override
    public void afterContainerStop(final TestResultContainer container) {
        containerListeners.forEach(listener -> listener.afterContainerStop(container));
    }

    @Override
    public void beforeContainerWrite(final TestResultContainer container) {
        containerListeners.forEach(listener -> listener.beforeContainerWrite(container));
    }

    @Override
    public void afterContainerWrite(final TestResultContainer container) {
        containerListeners.forEach(listener -> listener.afterContainerWrite(container));
    }

    @Override
    public void beforeFixtureStart(final FixtureResult result) {
        fixtureListeners.forEach(listener -> listener.beforeFixtureStart(result));
    }

    @Override
    public void afterFixtureStart(final FixtureResult result) {
        fixtureListeners.forEach(listener -> listener.afterFixtureStart(result));
    }

    @Override
    public void beforeFixtureUpdate(final FixtureResult result) {
        fixtureListeners.forEach(listener -> listener.beforeFixtureUpdate(result));
    }

    @Override
    public void afterFixtureUpdate(final FixtureResult result) {
        fixtureListeners.forEach(listener -> listener.afterFixtureUpdate(result));
    }

    @Override
    public void beforeFixtureStop(final FixtureResult result) {
        fixtureListeners.forEach(listener -> listener.beforeFixtureStop(result));
    }

    @Override
    public void afterFixtureStop(final FixtureResult result) {
        fixtureListeners.forEach(listener -> listener.afterFixtureStop(result));
    }

    @Override
    public void beforeStepStart(final StepResult result) {
        stepListeners.forEach(listener -> listener.beforeStepStart(result));
    }

    @Override
    public void afterStepStart(final StepResult result) {
        stepListeners.forEach(listener -> listener.afterStepStart(result));
    }

    @Override
    public void beforeStepUpdate(final StepResult result) {
        stepListeners.forEach(listener -> listener.beforeStepUpdate(result));
    }

    @Override
    public void afterStepUpdate(final StepResult result) {
        stepListeners.forEach(listener -> listener.afterStepUpdate(result));
    }

    @Override
    public void beforeStepStop(final StepResult result) {
        stepListeners.forEach(listener -> listener.beforeStepStop(result));
    }

    @Override
    public void afterStepStop(final StepResult result) {
        stepListeners.forEach(listener -> listener.afterStepStop(result));
    }
}
