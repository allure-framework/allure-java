package io.qameta.allure.listener;

import io.qameta.allure.model.FixtureResult;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * @since 2.0
 */
@SuppressWarnings("PMD.TooManyMethods")
public class LifecycleNotifier implements ContainerLifecycleListener,
        TestLifecycleListener, FixtureLifecycleListener, StepLifecycleListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(LifecycleNotifier.class);

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
        runSafely(testListeners, TestLifecycleListener::beforeTestSchedule, result);
    }

    @Override
    public void afterTestSchedule(final TestResult result) {
        runSafely(testListeners, TestLifecycleListener::afterTestSchedule, result);
    }

    @Override
    public void beforeTestUpdate(final TestResult result) {
        runSafely(testListeners, TestLifecycleListener::beforeTestUpdate, result);
    }

    @Override
    public void afterTestUpdate(final TestResult result) {
        runSafely(testListeners, TestLifecycleListener::afterTestUpdate, result);
    }

    @Override
    public void beforeTestStart(final TestResult result) {
        runSafely(testListeners, TestLifecycleListener::beforeTestStart, result);
    }

    @Override
    public void afterTestStart(final TestResult result) {
        runSafely(testListeners, TestLifecycleListener::afterTestStart, result);
    }

    @Override
    public void beforeTestStop(final TestResult result) {
        runSafely(testListeners, TestLifecycleListener::beforeTestStop, result);
    }

    @Override
    public void afterTestStop(final TestResult result) {
        runSafely(testListeners, TestLifecycleListener::afterTestStop, result);
    }

    @Override
    public void beforeTestWrite(final TestResult result) {
        runSafely(testListeners, TestLifecycleListener::beforeTestWrite, result);
    }

    @Override
    public void afterTestWrite(final TestResult result) {
        runSafely(testListeners, TestLifecycleListener::afterTestWrite, result);
    }

    @Override
    public void beforeContainerStart(final TestResultContainer container) {
        runSafely(containerListeners, ContainerLifecycleListener::beforeContainerStart, container);
    }

    @Override
    public void afterContainerStart(final TestResultContainer container) {
        runSafely(containerListeners, ContainerLifecycleListener::afterContainerStart, container);
    }

    @Override
    public void beforeContainerUpdate(final TestResultContainer container) {
        runSafely(containerListeners, ContainerLifecycleListener::beforeContainerUpdate, container);
    }

    @Override
    public void afterContainerUpdate(final TestResultContainer container) {
        runSafely(containerListeners, ContainerLifecycleListener::afterContainerUpdate, container);
    }

    @Override
    public void beforeContainerStop(final TestResultContainer container) {
        runSafely(containerListeners, ContainerLifecycleListener::beforeContainerStop, container);
    }

    @Override
    public void afterContainerStop(final TestResultContainer container) {
        runSafely(containerListeners, ContainerLifecycleListener::afterContainerStop, container);
    }

    @Override
    public void beforeContainerWrite(final TestResultContainer container) {
        runSafely(containerListeners, ContainerLifecycleListener::beforeContainerWrite, container);
    }

    @Override
    public void afterContainerWrite(final TestResultContainer container) {
        runSafely(containerListeners, ContainerLifecycleListener::afterContainerWrite, container);
    }

    @Override
    public void beforeFixtureStart(final FixtureResult result) {
        runSafely(fixtureListeners, FixtureLifecycleListener::beforeFixtureStart, result);
    }

    @Override
    public void afterFixtureStart(final FixtureResult result) {
        runSafely(fixtureListeners, FixtureLifecycleListener::afterFixtureStart, result);
    }

    @Override
    public void beforeFixtureUpdate(final FixtureResult result) {
        runSafely(fixtureListeners, FixtureLifecycleListener::beforeFixtureUpdate, result);
    }

    @Override
    public void afterFixtureUpdate(final FixtureResult result) {
        runSafely(fixtureListeners, FixtureLifecycleListener::afterFixtureUpdate, result);
    }

    @Override
    public void beforeFixtureStop(final FixtureResult result) {
        runSafely(fixtureListeners, FixtureLifecycleListener::beforeFixtureStop, result);
    }

    @Override
    public void afterFixtureStop(final FixtureResult result) {
        runSafely(fixtureListeners, FixtureLifecycleListener::afterFixtureStop, result);
    }

    @Override
    public void beforeStepStart(final StepResult result) {
        runSafely(stepListeners, StepLifecycleListener::beforeStepStart, result);
    }

    @Override
    public void afterStepStart(final StepResult result) {
        runSafely(stepListeners, StepLifecycleListener::afterStepStart, result);
    }

    @Override
    public void beforeStepUpdate(final StepResult result) {
        runSafely(stepListeners, StepLifecycleListener::beforeStepUpdate, result);
    }

    @Override
    public void afterStepUpdate(final StepResult result) {
        runSafely(stepListeners, StepLifecycleListener::afterStepUpdate, result);
    }

    @Override
    public void beforeStepStop(final StepResult result) {
        runSafely(stepListeners, StepLifecycleListener::beforeStepStop, result);
    }

    @Override
    public void afterStepStop(final StepResult result) {
        runSafely(stepListeners, StepLifecycleListener::afterStepStop, result);
    }

    protected <T extends LifecycleListener, S> void runSafely(final List<T> listeners,
                                                              final BiConsumer<T, S> method,
                                                              final S object) {
        listeners.forEach(listener -> {
            try {
                method.accept(listener, object);
            } catch (Exception e) {
                LOGGER.error("Could not invoke listener method", e);
            }
        });
    }

}
