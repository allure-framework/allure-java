/*
 *  Copyright 2016-2026 Qameta Software Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.qameta.allure.jbehave5;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.util.ResultsUtils;
import org.jbehave.core.failures.UUIDExceptionWrapper;
import org.jbehave.core.model.ExamplesTable;
import org.jbehave.core.model.Scenario;
import org.jbehave.core.model.Step;
import org.jbehave.core.model.Story;
import org.jbehave.core.reporters.NullStoryReporter;
import org.jbehave.core.steps.StepCreator;
import org.jbehave.core.steps.Timing;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static io.qameta.allure.util.ResultsUtils.bytesToHex;
import static io.qameta.allure.util.ResultsUtils.createFrameworkLabel;
import static io.qameta.allure.util.ResultsUtils.createHostLabel;
import static io.qameta.allure.util.ResultsUtils.createLanguageLabel;
import static io.qameta.allure.util.ResultsUtils.createParameter;
import static io.qameta.allure.util.ResultsUtils.createStoryLabel;
import static io.qameta.allure.util.ResultsUtils.createThreadLabel;
import static io.qameta.allure.util.ResultsUtils.createTitlePath;
import static io.qameta.allure.util.ResultsUtils.getMd5Digest;
import static io.qameta.allure.util.ResultsUtils.getStatus;
import static io.qameta.allure.util.ResultsUtils.getStatusDetails;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;

/**
 * Reports JBehave 5 story execution to Allure.
 *
 * <p>Configure this reporter in JBehave so story, scenario, example, and step events become Allure containers, test results, steps, labels, and parameters. Use the default lifecycle for normal runs or pass a lifecycle for tests and embedded runtimes.</p>
 */
public class AllureJbehave5 extends NullStoryReporter {

    private final AllureLifecycle lifecycle;

    private final ThreadLocal<Story> currentStory = new InheritableThreadLocal<>();

    private final ThreadLocal<Scenario> currentScenario = new InheritableThreadLocal<>();

    private final Map<Scenario, List<String>> scenarioUuids = new ConcurrentHashMap<>();

    private final ThreadLocal<Deque<Story>> givenStories = ThreadLocal.withInitial(LinkedList::new);

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Creates an Allure jbehave5 with default configuration.
     */
    @SuppressWarnings("unused")
    public AllureJbehave5() {
        this(Allure.getLifecycle());
    }

    /**
     * Creates an Allure jbehave5 with the supplied values.
     *
     * @param lifecycle the Allure lifecycle to use
     */
    public AllureJbehave5(final AllureLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeStory(final Story story, final boolean givenStory) {
        if (givenStory) {
            givenStories.get().push(story);
        } else {
            currentStory.set(story);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterStory(final boolean givenStory) {
        if (givenStory) {
            givenStories.get().pop();
        } else {
            currentStory.remove();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeScenario(final Scenario scenario) {
        if (isGivenStory()) {
            return;
        }
        currentScenario.set(scenario);

        if (notParameterised(scenario)) {
            final String uuid = UUID.randomUUID().toString();
            usingWriteLock(() -> scenarioUuids.put(scenario, new ArrayList<>(singletonList(uuid))));
            startTestCase(uuid, scenario, emptyMap());
        } else {
            usingWriteLock(() -> scenarioUuids.put(scenario, new ArrayList<>()));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeExamples(final List<String> steps, final ExamplesTable table) {
        if (isGivenStory()) {
            return;
        }
        final Scenario scenario = currentScenario.get();
        lock.writeLock().lock();
        try {
            scenarioUuids.put(scenario, new ArrayList<>());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void example(final Map<String, String> tableRow, final int exampleIndex) {
        if (isGivenStory()) {
            return;
        }
        final Scenario scenario = currentScenario.get();
        final String uuid = UUID.randomUUID().toString();
        usingWriteLock(() -> scenarioUuids.getOrDefault(scenario, new ArrayList<>()).add(uuid));
        startTestCase(uuid, scenario, tableRow);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterScenario(final Timing timing) {
        if (isGivenStory()) {
            return;
        }
        final Scenario scenario = currentScenario.get();
        usingReadLock(() -> {
            final List<String> uuids = scenarioUuids.getOrDefault(scenario, emptyList());
            uuids.forEach(this::stopTestCase);
        });
        currentScenario.remove();
        usingWriteLock(() -> scenarioUuids.remove(scenario));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeStep(final Step step) {
        final String stepUuid = UUID.randomUUID().toString();
        getLifecycle().startStep(stepUuid, new StepResult().setName(step.getStepAsString()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void successful(final String step) {
        getLifecycle().updateTestCase(result -> result.setStatus(Status.PASSED));
        getLifecycle().updateStep(result -> result.setStatus(Status.PASSED));
        getLifecycle().stopStep();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void ignorable(final String step) {
        getLifecycle().stopStep();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void pending(final StepCreator.PendingStep step) {
        getLifecycle().updateStep(result -> result.setStatus(Status.SKIPPED));
        getLifecycle().stopStep();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notPerformed(final String step) {
        getLifecycle().stopStep();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void failed(final String step, final Throwable cause) {
        final Throwable unwrapped = cause instanceof UUIDExceptionWrapper
                ? cause.getCause()
                : cause;

        final Status status = getStatus(unwrapped).orElse(Status.FAILED);
        final StatusDetails statusDetails = getStatusDetails(unwrapped).orElseGet(StatusDetails::new);

        getLifecycle().updateStep(result -> {
            result.setStatus(status);
            result.setStatusDetails(statusDetails);
        });

        getLifecycle().updateTestCase(result -> {
            result.setStatus(status);
            result.setStatusDetails(statusDetails);
        });

        getLifecycle().stopStep();
    }

    /**
     * Returns the lifecycle.
     *
     * @return the Allure lifecycle used by this integration
     */
    public AllureLifecycle getLifecycle() {
        return lifecycle;
    }

    /**
     * Handles the start test case callback.
     *
     * @param uuid the Allure UUID of the model object
     * @param scenario the scenario
     * @param tableRow the table row
     */
    protected void startTestCase(final String uuid,
                                 final Scenario scenario,
                                 final Map<String, String> tableRow) {
        final Story story = currentStory.get();

        final String name = scenario.getTitle();
        final String fullName = String.format("%s: %s", story.getName(), name);

        final List<Parameter> parameters = tableRow.entrySet().stream()
                .map(entry -> createParameter(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        final List<Label> labels = new ArrayList<>(
                Arrays.asList(
                        createStoryLabel(story.getName()),
                        createHostLabel(),
                        createThreadLabel(),
                        createFrameworkLabel("jbehave"),
                        createLanguageLabel("java")
                )
        );

        labels.addAll(ResultsUtils.getProvidedLabels());

        final String historyId = getHistoryId(fullName, parameters);

        final TestResult result = new TestResult()
                .setUuid(uuid)
                .setName(name)
                .setFullName(fullName)
                .setStage(Stage.SCHEDULED)
                .setTitlePath(getTitlePath(story))
                .setLabels(labels)
                .setParameters(parameters)
                .setDescription(story.getDescription().asString())
                .setHistoryId(historyId);

        getLifecycle().scheduleTestCase(result);
        getLifecycle().startTestCase(result.getUuid());
    }

    private List<String> getTitlePath(final Story story) {
        if (story.getPath() == null) {
            return createTitlePath();
        }
        return createTitlePath(Arrays.asList(story.getPath().replace('\\', '/').split("/")));
    }

    /**
     * Handles the stop test case callback.
     *
     * @param uuid the Allure UUID of the model object
     */
    protected void stopTestCase(final String uuid) {
        getLifecycle().stopTestCase(uuid);
        getLifecycle().writeTestCase(uuid);
    }

    /**
     * Returns the not parameterised.
     *
     * @param scenario the scenario
     * @return true when the condition is satisfied; false otherwise
     */
    protected boolean notParameterised(final Scenario scenario) {
        return scenario.getExamplesTable().getRowCount() == 0;
    }

    /**
     * Returns the history id.
     *
     * @param fullName the fully qualified test name
     * @param parameters the Allure parameters associated with the test
     * @return the history id
     */
    protected String getHistoryId(final String fullName, final List<Parameter> parameters) {
        final MessageDigest digest = getMd5Digest();
        digest.update(fullName.getBytes(UTF_8));
        parameters.stream()
                .sorted(comparing(Parameter::getName).thenComparing(Parameter::getValue))
                .forEachOrdered(parameter -> {
                    digest.update(parameter.getName().getBytes(UTF_8));
                    digest.update(parameter.getValue().getBytes(UTF_8));
                });
        final byte[] bytes = digest.digest();
        return bytesToHex(bytes);
    }

    private boolean isGivenStory() {
        return !givenStories.get().isEmpty();
    }

    private void usingReadLock(final Runnable runnable) {
        lock.readLock().lock();
        try {
            runnable.run();
        } finally {
            lock.readLock().unlock();
        }
    }

    private void usingWriteLock(final Runnable runnable) {
        lock.writeLock().lock();
        try {
            runnable.run();
        } finally {
            lock.writeLock().unlock();
        }
    }

}
