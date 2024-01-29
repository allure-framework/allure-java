/*
 *  Copyright 2016-2024 Qameta Software Inc
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
package io.qameta.allure.jbehave;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import org.jbehave.core.failures.UUIDExceptionWrapper;
import org.jbehave.core.model.ExamplesTable;
import org.jbehave.core.model.Scenario;
import org.jbehave.core.model.Story;
import org.jbehave.core.reporters.NullStoryReporter;

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
import static io.qameta.allure.util.ResultsUtils.getMd5Digest;
import static io.qameta.allure.util.ResultsUtils.getStatus;
import static io.qameta.allure.util.ResultsUtils.getStatusDetails;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;

/**
 * @author charlie (Dmitry Baev).
 */
public class AllureJbehave extends NullStoryReporter {

    private final AllureLifecycle lifecycle;

    private final ThreadLocal<Story> currentStory = new InheritableThreadLocal<>();

    private final ThreadLocal<Scenario> currentScenario = new InheritableThreadLocal<>();

    private final Map<Scenario, List<String>> scenarioUuids = new ConcurrentHashMap<>();

    private final ThreadLocal<Deque<Story>> givenStories = ThreadLocal.withInitial(LinkedList::new);

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    @SuppressWarnings("unused")
    public AllureJbehave() {
        this(Allure.getLifecycle());
    }

    public AllureJbehave(final AllureLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    @Override
    public void beforeStory(final Story story, final boolean givenStory) {
        if (givenStory) {
            givenStories.get().push(story);
        } else {
            currentStory.set(story);
        }
    }

    @Override
    public void afterStory(final boolean givenStory) {
        if (givenStory) {
            givenStories.get().pop();
        } else {
            currentStory.remove();
        }
    }

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

    @Override
    public void afterScenario() {
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

    @Override
    public void beforeStep(final String step) {
        final String stepUuid = UUID.randomUUID().toString();
        getLifecycle().startStep(stepUuid, new StepResult().setName(step));
    }

    @Override
    public void successful(final String step) {
        getLifecycle().updateTestCase(result -> result.setStatus(Status.PASSED));
        getLifecycle().updateStep(result -> result.setStatus(Status.PASSED));
        getLifecycle().stopStep();
    }

    @Override
    public void ignorable(final String step) {
        beforeStep(step);
        getLifecycle().stopStep();
    }

    @Override
    public void pending(final String step) {
        beforeStep(step);
        getLifecycle().updateStep(result -> result.setStatus(Status.SKIPPED));
        getLifecycle().stopStep();
    }

    @Override
    public void notPerformed(final String step) {
        beforeStep(step);
        getLifecycle().stopStep();
    }

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


    public AllureLifecycle getLifecycle() {
        return lifecycle;
    }

    protected void startTestCase(final String uuid,
                                 final Scenario scenario,
                                 final Map<String, String> tableRow) {
        final Story story = currentStory.get();

        final String name = scenario.getTitle();
        final String fullName = String.format("%s: %s", story.getName(), name);

        final List<Parameter> parameters = tableRow.entrySet().stream()
                .map(entry -> createParameter(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        final List<Label> labels = Arrays.asList(
                createStoryLabel(story.getName()),
                createHostLabel(),
                createThreadLabel(),
                createFrameworkLabel("jbehave"),
                createLanguageLabel("java")
        );

        final String historyId = getHistoryId(fullName, parameters);

        final TestResult result = new TestResult()
                .setUuid(uuid)
                .setName(name)
                .setFullName(fullName)
                .setStage(Stage.SCHEDULED)
                .setLabels(labels)
                .setParameters(parameters)
                .setDescription(story.getDescription().asString())
                .setHistoryId(historyId);

        getLifecycle().scheduleTestCase(result);
        getLifecycle().startTestCase(result.getUuid());
    }

    protected void stopTestCase(final String uuid) {
        getLifecycle().stopTestCase(uuid);
        getLifecycle().writeTestCase(uuid);
    }

    protected boolean notParameterised(final Scenario scenario) {
        return scenario.getExamplesTable().getRowCount() == 0;
    }

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
