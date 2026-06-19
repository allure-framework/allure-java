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
package io.qameta.allure;

import io.qameta.allure.http.HttpExchange;
import io.qameta.allure.http.HttpExchangeSerializer;
import io.qameta.allure.internal.AllureStorage;
import io.qameta.allure.internal.AllureThreadContext;
import io.qameta.allure.listener.ContainerLifecycleListener;
import io.qameta.allure.listener.FixtureLifecycleListener;
import io.qameta.allure.listener.LifecycleNotifier;
import io.qameta.allure.listener.StepLifecycleListener;
import io.qameta.allure.listener.TestLifecycleListener;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.FixtureResult;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.ScopeFixtureResult;
import io.qameta.allure.model.ScopeFixtureType;
import io.qameta.allure.model.ScopeResult;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;
import io.qameta.allure.model.WithAttachments;
import io.qameta.allure.model.WithSteps;
import io.qameta.allure.util.PropertiesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

import static io.qameta.allure.AllureConstants.ATTACHMENT_FILE_SUFFIX;
import static io.qameta.allure.util.ServiceLoaderUtils.load;

/**
 * The class contains Allure context and methods to change it.
 *
 * <p>Integration adapters should model suite-level grouping through {@link ScopeResult} by using
 * {@link #startScope(ScopeResult)}, {@link #startScope(String, ScopeResult)}, {@link #updateScope(String, Consumer)},
 * {@link #stopScope(String)}, and {@link #writeScope(String)}. The test-container methods are retained only as
 * migration bridges for existing adapters and are deprecated for removal.</p>
 */
@SuppressWarnings({"PMD.AvoidSynchronizedStatement", "PMD.GodClass", "PMD.TooManyMethods"})
public class AllureLifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureLifecycle.class);

    private final AllureResultsWriter writer;

    private final AllureStorage storage;

    private final AllureThreadContext threadContext;

    private final LifecycleNotifier notifier;

    private final Map<String, FixtureContext> fixtureContexts = new ConcurrentHashMap<>();

    private final Map<String, ScopeResult> scopes = new ConcurrentHashMap<>();

    private final Map<String, ScopeMetadata> scopeMetadata = new ConcurrentHashMap<>();

    private final Map<String, String> scopeParents = new ConcurrentHashMap<>();

    private final Map<String, Set<String>> testScopes = new ConcurrentHashMap<>();

    /**
     * Creates a new lifecycle with default results writer. Shortcut
     * for {@link #AllureLifecycle(AllureResultsWriter)}
     */
    public AllureLifecycle() {
        this(getDefaultWriter());
    }

    /**
     * Creates a new lifecycle instance with specified {@link AllureResultsWriter}.
     *
     * @param writer the results writer.
     */
    public AllureLifecycle(final AllureResultsWriter writer) {
        this(writer, getDefaultNotifier());
    }

    /**
     * Creates a new lifecycle instance with specified {@link AllureResultsWriter}
     * and {@link LifecycleNotifier}.
     *
     * @param writer the results writer.
     */
    AllureLifecycle(final AllureResultsWriter writer, final LifecycleNotifier lifecycleNotifier) {
        this.notifier = lifecycleNotifier;
        this.writer = writer;
        this.storage = new AllureStorage();
        this.threadContext = new AllureThreadContext();
    }

    /**
     * Starts scope with specified parent scope.
     *
     * @param parentScopeUuid the uuid of parent scope.
     * @param scope           the scope.
     */
    public void startScope(final String parentScopeUuid, final ScopeResult scope) {
        scopeParents.put(scope.getUuid(), parentScopeUuid);
        final ScopeResult parentScope = scopes.get(parentScopeUuid);
        if (Objects.nonNull(parentScope)) {
            synchronized (parentScope) {
                normalizeScope(parentScope);
                parentScope.getChildScopes().add(scope.getUuid());
            }
        } else {
            storage.getContainer(parentScopeUuid).ifPresent(parent -> {
                synchronized (storage) {
                    parent.getChildren().add(scope.getUuid());
                }
            });
        }
        startScope(scope);
    }

    /**
     * Starts scope.
     *
     * @param scope the scope.
     */
    public void startScope(final ScopeResult scope) {
        normalizeScope(scope);
        scopes.put(scope.getUuid(), scope);
        linkExistingTests(scope);
    }

    /**
     * Starts test container with specified parent container.
     *
     * @deprecated use {@link #startScope(String, ScopeResult)} instead. This method is a migration bridge for
     * existing adapters and is planned for removal after the 3.x transition.
     *
     * @param containerUuid the uuid of parent container.
     * @param container     the container.
     */
    @Deprecated(
            since = "3.0.0",
            forRemoval = true
    )
    public void startTestContainer(final String containerUuid, final TestResultContainer container) {
        scopeParents.put(container.getUuid(), containerUuid);
        storage.getContainer(containerUuid).ifPresent(parent -> {
            synchronized (storage) {
                parent.getChildren().add(container.getUuid());
            }
        });
        startContainer(container);
    }

    /**
     * Starts test container.
     *
     * @deprecated use {@link #startScope(ScopeResult)} instead. This method is a migration bridge for existing
     * adapters and is planned for removal after the 3.x transition.
     *
     * @param container the container.
     */
    @Deprecated(
            since = "3.0.0",
            forRemoval = true
    )
    public void startTestContainer(final TestResultContainer container) {
        startContainer(container);
    }

    private void startContainer(final TestResultContainer container) {
        notifier.beforeContainerStart(container);
        container.setStart(System.currentTimeMillis());
        storage.put(container.getUuid(), container);
        scopeMetadata.computeIfAbsent(container.getUuid(), key -> new ScopeMetadata());
        linkExistingChildren(container);
        notifier.afterContainerStart(container);
    }

    /**
     * Updates scope.
     *
     * @param uuid   the uuid of scope.
     * @param update the update function.
     */
    public void updateScope(final String uuid, final Consumer<ScopeResult> update) {
        final ScopeResult scope = scopes.get(uuid);
        if (Objects.isNull(scope)) {
            LOGGER.error("Could not update scope: scope with uuid {} not found", uuid);
            return;
        }
        synchronized (scope) {
            update.accept(scope);
            normalizeScope(scope);
            linkExistingTests(scope);
        }
    }

    /**
     * Updates test container.
     *
     * @deprecated use {@link #updateScope(String, Consumer)} instead. This method is a migration bridge for
     * existing adapters and is planned for removal after the 3.x transition.
     *
     * @param uuid   the uuid of container.
     * @param update the update function.
     */
    @Deprecated(
            since = "3.0.0",
            forRemoval = true
    )
    public void updateTestContainer(final String uuid, final Consumer<TestResultContainer> update) {
        final Optional<TestResultContainer> found = storage.getContainer(uuid);
        if (!found.isPresent()) {
            LOGGER.error("Could not update test container: container with uuid {} not found", uuid);
            return;
        }
        final TestResultContainer container = found.get();
        notifier.beforeContainerUpdate(container);
        update.accept(container);
        linkExistingChildren(container);
        notifier.afterContainerUpdate(container);
    }

    /**
     * Stops scope by given uuid.
     *
     * @param uuid the uuid of scope.
     */
    public void stopScope(final String uuid) {
        if (!scopes.containsKey(uuid)) {
            LOGGER.error("Could not stop scope: scope with uuid {} not found", uuid);
        }
    }

    /**
     * Stops test container by given uuid.
     *
     * @deprecated use {@link #stopScope(String)} instead. This method is a migration bridge for existing adapters
     * and is planned for removal after the 3.x transition.
     *
     * @param uuid the uuid of container.
     */
    @Deprecated(
            since = "3.0.0",
            forRemoval = true
    )
    public void stopTestContainer(final String uuid) {
        stopContainer(uuid);
    }

    /**
     * Writes scope with given uuid.
     *
     * @param uuid the uuid of scope.
     */
    public void writeScope(final String uuid) {
        final ScopeResult scope = scopes.get(uuid);
        if (Objects.isNull(scope)) {
            LOGGER.error("Could not write scope: scope with uuid {} not found", uuid);
            return;
        }

        final TestResultContainer container;
        synchronized (scope) {
            normalizeScope(scope);
            container = toScopeContainer(scope);
        }
        writeContainer(container);

        scopes.remove(uuid);
        scopeParents.remove(uuid);
    }

    /**
     * Writes test container with given uuid.
     *
     * @deprecated use {@link #writeScope(String)} instead. This method is a migration bridge for existing adapters
     * and is planned for removal after the 3.x transition.
     *
     * @param uuid the uuid of container.
     */
    @Deprecated(
            since = "3.0.0",
            forRemoval = true
    )
    public void writeTestContainer(final String uuid) {
        writeContainer(uuid);
    }

    /**
     * Start a new before fixture with given scope.
     *
     * @param scopeUuid the uuid of owning scope.
     * @param uuid      the fixture uuid.
     * @param result    the fixture.
     */
    public void startBeforeFixture(final String scopeUuid, final String uuid, final FixtureResult result) {
        addFixtureToScopeOrContainer(scopeUuid, uuid, result, ScopeFixtureType.BEFORE);
        notifier.beforeFixtureStart(result);
        startFixture(uuid, result, new FixtureContext(scopeUuid, ScopeFixtureType.BEFORE, threadContext.copy()));
        notifier.afterFixtureStart(result);
    }

    /**
     * Start a new prepare fixture with given parent.
     *
     * @deprecated use {@link #startBeforeFixture(String, String, FixtureResult)} instead. This method is a
     * migration bridge for existing adapters and is planned for removal after the 3.x transition.
     *
     * @param containerUuid the uuid of parent container.
     * @param uuid          the fixture uuid.
     * @param result        the fixture.
     */
    @Deprecated(
            since = "3.0.0",
            forRemoval = true
    )
    public void startPrepareFixture(final String containerUuid, final String uuid, final FixtureResult result) {
        startBeforeFixture(containerUuid, uuid, result);
    }

    /**
     * Start a new after fixture with given scope.
     *
     * @param scopeUuid the uuid of owning scope.
     * @param uuid      the fixture uuid.
     * @param result    the fixture.
     */
    public void startAfterFixture(final String scopeUuid, final String uuid, final FixtureResult result) {
        addFixtureToScopeOrContainer(scopeUuid, uuid, result, ScopeFixtureType.AFTER);
        notifier.beforeFixtureStart(result);
        startFixture(uuid, result, new FixtureContext(scopeUuid, ScopeFixtureType.AFTER, threadContext.copy()));
        notifier.afterFixtureStart(result);
    }

    /**
     * Start a new tear down fixture with given parent.
     *
     * @deprecated use {@link #startAfterFixture(String, String, FixtureResult)} instead. This method is a
     * migration bridge for existing adapters and is planned for removal after the 3.x transition.
     *
     * @param containerUuid the uuid of parent container.
     * @param uuid          the fixture uuid.
     * @param result        the fixture.
     */
    @Deprecated(
            since = "3.0.0",
            forRemoval = true
    )
    public void startTearDownFixture(final String containerUuid, final String uuid, final FixtureResult result) {
        startAfterFixture(containerUuid, uuid, result);
    }

    /**
     * Start a new fixture with given uuid.
     *
     * @param uuid   the uuid of fixture.
     * @param result the test fixture.
     */
    private void startFixture(final String uuid, final FixtureResult result, final FixtureContext context) {
        fixtureContexts.put(uuid, context);
        storage.put(uuid, result);
        result.setStage(Stage.RUNNING);
        result.setStart(System.currentTimeMillis());
        threadContext.clear();
        threadContext.start(uuid);
    }

    /**
     * Updates current running fixture. Shortcut for {@link #updateFixture(String, Consumer)}.
     *
     * @param update the update function.
     */
    public void updateFixture(final Consumer<FixtureResult> update) {
        final Optional<String> root = threadContext.getRoot();
        if (!root.isPresent()) {
            LOGGER.error("Could not update test fixture: no test fixture running");
            return;
        }
        final String uuid = root.get();
        updateFixture(uuid, update);
    }

    /**
     * Updates fixture by given uuid.
     *
     * @param uuid   the uuid of fixture.
     * @param update the update function.
     */
    public void updateFixture(final String uuid, final Consumer<FixtureResult> update) {
        final Optional<FixtureResult> found = storage.getFixture(uuid);
        if (!found.isPresent()) {
            LOGGER.error("Could not update test fixture: test fixture with uuid {} not found", uuid);
            return;
        }
        final FixtureResult fixture = found.get();

        notifier.beforeFixtureUpdate(fixture);
        update.accept(fixture);
        notifier.afterFixtureUpdate(fixture);
    }

    /**
     * Stops fixture by given uuid.
     *
     * @param uuid the uuid of fixture.
     */
    public void stopFixture(final String uuid) {
        final Optional<FixtureResult> found = storage.getFixture(uuid);
        if (!found.isPresent()) {
            LOGGER.error("Could not stop test fixture: test fixture with uuid {} not found", uuid);
            return;
        }
        final FixtureResult fixture = found.get();

        notifier.beforeFixtureStop(fixture);
        fixture.setStage(Stage.FINISHED);
        fixture.setStop(System.currentTimeMillis());

        storage.remove(uuid);
        restoreContextAfterFixture(uuid);

        notifier.afterFixtureStop(fixture);
    }

    /**
     * Returns uuid of current running test case if any.
     *
     * @return the uuid of current running test case.
     */
    public Optional<String> getCurrentTestCase() {
        return threadContext.getRoot()
                .filter(uuid -> storage.getTestResult(uuid).isPresent());
    }

    /**
     * Returns uuid of current running test case or step if any.
     *
     * @return the uuid of current running test case or step.
     */
    public Optional<String> getCurrentTestCaseOrStep() {
        return threadContext.getCurrent();
    }

    /**
     * Sets specified test case uuid as current. Note that
     * test case with such uuid should be created and existed in storage, otherwise
     * method take no effect.
     *
     * @param uuid the uuid of test case.
     * @return true if current test case was configured successfully, false otherwise.
     */
    public boolean setCurrentTestCase(final String uuid) {
        final Optional<TestResult> found = storage.getTestResult(uuid);
        if (!found.isPresent()) {
            return false;
        }
        threadContext.clear();
        threadContext.start(uuid);
        return true;
    }

    /**
     * Schedules test case with given scope.
     *
     * @param containerUuid the uuid of scope.
     * @param result        the test case to schedule.
     */
    public void scheduleTestCase(final String containerUuid, final TestResult result) {
        final ScopeResult scope = scopes.get(containerUuid);
        if (Objects.nonNull(scope)) {
            addTestChildToScope(containerUuid, result.getUuid());
        } else {
            storage.getContainer(containerUuid).ifPresent(container -> {
                synchronized (storage) {
                    container.getChildren().add(result.getUuid());
                }
            });
        }
        linkTestToScope(containerUuid, result.getUuid());
        scheduleTestCase(result);
    }

    /**
     * Schedule given test case.
     *
     * @param result the test case to schedule.
     */
    public void scheduleTestCase(final TestResult result) {
        notifier.beforeTestSchedule(result);
        result.setStage(Stage.SCHEDULED);
        storage.put(result.getUuid(), result);
        notifier.afterTestSchedule(result);
    }

    /**
     * Starts test case with given uuid. In order to start test case it should be scheduled at first.
     *
     * @param uuid the uuid of test case to start.
     */
    public void startTestCase(final String uuid) {
        threadContext.clear();
        final Optional<TestResult> found = storage.getTestResult(uuid);
        if (!found.isPresent()) {
            LOGGER.error("Could not start test case: test case with uuid {} is not scheduled", uuid);
            return;
        }
        final TestResult testResult = found.get();

        notifier.beforeTestStart(testResult);
        testResult
                .setStage(Stage.RUNNING)
                .setStart(System.currentTimeMillis());
        threadContext.start(uuid);
        notifier.afterTestStart(testResult);
    }

    /**
     * Shortcut for {@link #updateTestCase(String, Consumer)} for current running test case uuid.
     *
     * @param update the update function.
     */
    public void updateTestCase(final Consumer<TestResult> update) {
        final Optional<String> root = threadContext.getRoot();
        if (!root.isPresent()) {
            LOGGER.error("Could not update test case: no test case running");
            return;
        }

        final String uuid = root.get();
        updateTestCase(uuid, update);
    }

    /**
     * Updates test case by given uuid.
     *
     * @param uuid   the uuid of test case to update.
     * @param update the update function.
     */
    public void updateTestCase(final String uuid, final Consumer<TestResult> update) {
        final Optional<TestResult> found = storage.getTestResult(uuid);
        if (!found.isPresent()) {
            LOGGER.error("Could not update test case: test case with uuid {} not found", uuid);
            return;
        }
        final TestResult testResult = found.get();

        notifier.beforeTestUpdate(testResult);
        update.accept(testResult);
        notifier.afterTestUpdate(testResult);
    }

    /**
     * Stops test case by given uuid. Test case marked as {@link Stage#FINISHED} and also
     * stop timestamp is calculated. Result would be stored in memory until
     * {@link #writeTestCase(String)} method is called. Also stopped test case could be
     * updated by {@link #updateTestCase(String, Consumer)} method.
     *
     * @param uuid the uuid of test case to stop.
     */
    public void stopTestCase(final String uuid) {
        final Optional<TestResult> found = storage.getTestResult(uuid);
        if (!found.isPresent()) {
            LOGGER.error("Could not stop test case: test case with uuid {} not found", uuid);
            return;
        }
        final TestResult testResult = found.get();

        notifier.beforeTestStop(testResult);
        testResult
                .setStage(Stage.FINISHED)
                .setStop(System.currentTimeMillis());
        applyScopeMetadata(testResult);
        threadContext.clear();
        notifier.afterTestStop(testResult);
    }

    /**
     * Writes test case with given uuid using configured {@link AllureResultsWriter}.
     *
     * @param uuid the uuid of test case to write.
     */
    public void writeTestCase(final String uuid) {
        final Optional<TestResult> found = storage.getTestResult(uuid);
        if (!found.isPresent()) {
            LOGGER.error("Could not write test case: test case with uuid {} not found", uuid);
            return;
        }

        final TestResult testResult = found.get();
        notifier.beforeTestWrite(testResult);
        writer.write(testResult);
        storage.remove(uuid);
        testScopes.remove(uuid);
        notifier.afterTestWrite(testResult);
    }

    /**
     * Adds metadata label to the current test or current before-fixture scope.
     *
     * @param label the label
     */
    public void addLabel(final Label label) {
        updateTestOrBeforeFixtureScope(
                "label",
                testResult -> testResult.getLabels().add(label),
                scope -> scope.getLabels().add(label),
                metadata -> metadata.getLabels().add(label)
        );
    }

    /**
     * Adds metadata link to the current test or current before-fixture scope.
     *
     * @param link the link
     */
    public void addLink(final Link link) {
        updateTestOrBeforeFixtureScope(
                "link",
                testResult -> testResult.getLinks().add(link),
                scope -> scope.getLinks().add(link),
                metadata -> metadata.getLinks().add(link)
        );
    }

    /**
     * Adds metadata parameter to the current test or current before-fixture scope.
     *
     * @param parameter the parameter
     */
    public void addParameter(final Parameter parameter) {
        updateTestOrBeforeFixtureScope(
                "parameter",
                testResult -> testResult.getParameters().add(parameter),
                scope -> scope.getParameters().add(parameter),
                metadata -> metadata.getParameters().add(parameter)
        );
    }

    /**
     * Sets description on the current test or current before-fixture scope.
     *
     * @param description the description
     */
    public void setDescription(final String description) {
        updateTestOrBeforeFixtureScope(
                "description",
                testResult -> testResult.setDescription(description),
                scope -> scope.setDescription(description),
                metadata -> metadata.setDescription(description)
        );
    }

    /**
     * Sets HTML description on the current test or current before-fixture scope.
     *
     * @param descriptionHtml the HTML description
     */
    public void setDescriptionHtml(final String descriptionHtml) {
        updateTestOrBeforeFixtureScope(
                "descriptionHtml",
                testResult -> testResult.setDescriptionHtml(descriptionHtml),
                scope -> scope.setDescriptionHtml(descriptionHtml),
                metadata -> metadata.setDescriptionHtml(descriptionHtml)
        );
    }

    /**
     * Start a new step as child step of current running test case or step. Shortcut
     * for {@link #startStep(String, String, StepResult)}.
     *
     * @param uuid   the uuid of step.
     * @param result the step.
     */
    public void startStep(final String uuid, final StepResult result) {
        final Optional<String> current = threadContext.getCurrent();
        if (!current.isPresent()) {
            LOGGER.error("Could not start step: no test case running");
            return;
        }
        final String parentUuid = current.get();
        startStep(parentUuid, uuid, result);
    }

    /**
     * Start a new step as child of specified parent.
     *
     * @param parentUuid the uuid of parent test case or step.
     * @param uuid       the uuid of step.
     * @param result     the step.
     */
    public void startStep(final String parentUuid, final String uuid, final StepResult result) {
        notifier.beforeStepStart(result);

        result.setStage(Stage.RUNNING);
        result.setStart(System.currentTimeMillis());

        threadContext.start(uuid);

        storage.put(uuid, result);
        storage.get(parentUuid, WithSteps.class).ifPresent(parentStep -> {
            synchronized (storage) {
                parentStep.getSteps().add(result);
            }
        });

        notifier.afterStepStart(result);
    }

    /**
     * Updates current step. Shortcut for {@link #updateStep(String, Consumer)}.
     *
     * @param update the update function.
     */
    public void updateStep(final Consumer<StepResult> update) {
        final Optional<String> current = threadContext.getCurrent();
        if (!current.isPresent()) {
            LOGGER.error("Could not update step: no step running");
            return;
        }
        final String uuid = current.get();
        updateStep(uuid, update);
    }

    /**
     * Updates step by specified uuid.
     *
     * @param uuid   the uuid of step.
     * @param update the update function.
     */
    public void updateStep(final String uuid, final Consumer<StepResult> update) {
        final Optional<StepResult> found = storage.getStep(uuid);
        if (!found.isPresent()) {
            LOGGER.error("Could not update step: step with uuid {} not found", uuid);
            return;
        }

        final StepResult step = found.get();

        notifier.beforeStepUpdate(step);
        update.accept(step);
        notifier.afterStepUpdate(step);
    }

    /**
     * Stops current running step. Shortcut for {@link #stopStep(String)}.
     */
    public void stopStep() {
        final String root = threadContext.getRoot().orElse(null);
        final Optional<String> current = threadContext.getCurrent()
                .filter(uuid -> !Objects.equals(uuid, root));
        if (!current.isPresent()) {
            LOGGER.error("Could not stop step: no step running");
            return;
        }
        final String uuid = current.get();
        stopStep(uuid);
    }

    /**
     * Stops step by given uuid.
     *
     * @param uuid the uuid of step to stop.
     */
    public void stopStep(final String uuid) {
        final Optional<StepResult> found = storage.getStep(uuid);
        if (!found.isPresent()) {
            LOGGER.error("Could not stop step: step with uuid {} not found", uuid);
            return;
        }

        final StepResult step = found.get();
        notifier.beforeStepStop(step);

        step.setStage(Stage.FINISHED);
        step.setStop(System.currentTimeMillis());

        storage.remove(uuid);
        threadContext.stop();

        notifier.afterStepStop(step);
    }

    /**
     * Adds attachment into current test or step if any exists. Shortcut
     * for {@link #addAttachment(String, String, String, InputStream)}
     *
     * @param name          the name of attachment
     * @param type          the content type of attachment
     * @param fileExtension the attachment file extension
     * @param body          attachment content
     */
    public void addAttachment(final String name, final String type,
                              final String fileExtension, final byte[] body) {
        addAttachment(name, type, fileExtension, new ByteArrayInputStream(body));
    }

    /**
     * Adds attachment to current running test or step.
     *
     * @param name          the name of attachment
     * @param type          the content type of attachment
     * @param fileExtension the attachment file extension
     * @param stream        attachment content
     */
    public void addAttachment(final String name, final String type,
                              final String fileExtension, final InputStream stream) {
        writeAttachment(prepareAttachment(name, type, fileExtension), stream);
    }

    /**
     * Adds an HTTP exchange attachment.
     *
     * <p>Build the exchange with the desired capture options before calling this method. This method only
     * writes the already captured exchange.</p>
     *
     * @param name the attachment name
     * @param exchange the HTTP exchange payload
     */
    public void addHttpExchange(final String name, final HttpExchange exchange) {
        addAttachment(
                name,
                HttpExchange.CONTENT_TYPE,
                HttpExchange.FILE_EXTENSION,
                HttpExchangeSerializer.toJsonBytes(exchange)
        );
    }

    /**
     * Adds attachment to current running test or step, and returns source. In order
     * to store attachment content use {@link #writeAttachment(String, InputStream)} method.
     *
     * @param name          the name of attachment
     * @param type          the content type of attachment
     * @param fileExtension the attachment file extension
     * @return the source of added attachment
     */
    public String prepareAttachment(final String name, final String type, final String fileExtension) {
        final String extension = Optional.ofNullable(fileExtension)
                .filter(ext -> !ext.isEmpty())
                .map(ext -> ext.charAt(0) == '.' ? ext : "." + ext)
                .orElse("");
        final String source = UUID.randomUUID() + ATTACHMENT_FILE_SUFFIX + extension;

        final Optional<String> current = threadContext.getCurrent();
        if (!current.isPresent()) {
            LOGGER.error("Could not add attachment: no test is running");
            //backward compatibility: return source even if no attachment is going to be written.
            return source;
        }
        final Attachment attachment = new Attachment()
                .setName(isEmpty(name) ? null : name)
                .setType(isEmpty(type) ? null : type)
                .setSource(source);

        final String uuid = current.get();
        storage.get(uuid, WithAttachments.class).ifPresent(withAttachments -> {
            synchronized (storage) {
                withAttachments.getAttachments().add(attachment);
            }
        });
        return attachment.getSource();
    }

    /**
     * Writes attachment with specified source.
     *
     * @param attachmentSource the source of attachment.
     * @param stream           the attachment content.
     */
    public void writeAttachment(final String attachmentSource, final InputStream stream) {
        writer.write(attachmentSource, stream);
    }

    private void addFixtureToScopeOrContainer(final String scopeUuid, final String uuid,
                                              final FixtureResult result, final ScopeFixtureType type) {
        final ScopeResult scope = scopes.get(scopeUuid);
        if (Objects.nonNull(scope)) {
            synchronized (scope) {
                normalizeScope(scope);
                scope.getFixtures().add(
                        new ScopeFixtureResult()
                                .setUuid(uuid)
                                .setValue(result)
                                .setType(type)
                                .setScopeUuid(scopeUuid)
                );
            }
            return;
        }
        storage.getContainer(scopeUuid).ifPresent(container -> {
            synchronized (storage) {
                if (ScopeFixtureType.BEFORE.equals(type)) {
                    container.getBefores().add(result);
                } else if (ScopeFixtureType.AFTER.equals(type)) {
                    container.getAfters().add(result);
                }
            }
        });
    }

    private void stopContainer(final String uuid) {
        final Optional<TestResultContainer> found = storage.getContainer(uuid);
        if (!found.isPresent()) {
            LOGGER.error("Could not stop test container: container with uuid {} not found", uuid);
            return;
        }
        final TestResultContainer container = found.get();
        notifier.beforeContainerStop(container);
        container.setStop(System.currentTimeMillis());
        notifier.afterContainerStop(container);
    }

    private void writeContainer(final String uuid) {
        final Optional<TestResultContainer> found = storage.getContainer(uuid);
        if (!found.isPresent()) {
            LOGGER.error("Could not write test container: container with uuid {} not found", uuid);
            return;
        }
        final TestResultContainer container = found.get();
        writeContainer(container);

        storage.remove(uuid);
        scopeMetadata.remove(uuid);
        scopeParents.remove(uuid);
    }

    private void writeContainer(final TestResultContainer container) {
        notifier.beforeContainerWrite(container);
        writer.write(container);
        notifier.afterContainerWrite(container);
    }

    private TestResultContainer toScopeContainer(final ScopeResult scope) {
        final List<String> children = new ArrayList<>();
        children.addAll(scope.getChildScopes());
        children.addAll(scope.getTestChildren());
        if (children.isEmpty()) {
            children.addAll(scope.getTests());
        }
        final TestResultContainer container = new TestResultContainer()
                .setUuid(scope.getUuid())
                .setName(scope.getName())
                .setChildren(new ArrayList<>(new LinkedHashSet<>(children)));
        for (ScopeFixtureResult fixture : scope.getFixtures()) {
            final FixtureResult result = fixture.getValue();
            if (Objects.isNull(result)) {
                continue;
            }
            if (ScopeFixtureType.BEFORE.equals(fixture.getType())) {
                container.getBefores().add(result);
            } else if (ScopeFixtureType.AFTER.equals(fixture.getType())) {
                container.getAfters().add(result);
            }
        }
        return container;
    }

    private void normalizeScope(final ScopeResult scope) {
        scope.setTests(mutableList(scope.getTests()));
        scope.setChildScopes(mutableList(scope.getChildScopes()));
        scope.setTestChildren(mutableList(scope.getTestChildren()));
        scope.setFixtures(mutableList(scope.getFixtures()));
        scope.setLabels(mutableList(scope.getLabels()));
        scope.setLinks(mutableList(scope.getLinks()));
        scope.setParameters(mutableList(scope.getParameters()));
    }

    private <T> List<T> mutableList(final List<T> values) {
        return Objects.isNull(values) ? new ArrayList<>() : new ArrayList<>(values);
    }

    private void updateTestOrBeforeFixtureScope(final String metadataName,
                                                final Consumer<TestResult> testUpdate,
                                                final Consumer<ScopeResult> scopeUpdate,
                                                final Consumer<ScopeMetadata> legacyScopeUpdate) {
        final Optional<String> root = threadContext.getRoot();
        if (!root.isPresent()) {
            LOGGER.error("Could not update test metadata: no test or before fixture running");
            return;
        }

        final String uuid = root.get();
        final Optional<TestResult> testResult = storage.getTestResult(uuid);
        if (testResult.isPresent()) {
            updateTestCase(uuid, testUpdate);
            return;
        }

        final FixtureContext fixtureContext = fixtureContexts.get(uuid);
        if (Objects.isNull(fixtureContext)) {
            LOGGER.error("Could not add {} metadata: current root {} is not a test or fixture", metadataName, uuid);
            return;
        }

        if (ScopeFixtureType.AFTER.equals(fixtureContext.type())) {
            LOGGER.error("Could not add {} metadata: after fixture metadata is not supported", metadataName);
            return;
        }

        final ScopeResult scope = scopes.get(fixtureContext.scopeUuid());
        if (Objects.nonNull(scope)) {
            synchronized (scope) {
                normalizeScope(scope);
                scopeUpdate.accept(scope);
            }
            return;
        }

        final ScopeMetadata metadata = scopeMetadata.computeIfAbsent(
                fixtureContext.scopeUuid(),
                key -> new ScopeMetadata()
        );
        synchronized (metadata) {
            legacyScopeUpdate.accept(metadata);
        }
    }

    private void applyScopeMetadata(final TestResult testResult) {
        final Set<String> linkedScopes = testScopes.get(testResult.getUuid());
        if (Objects.isNull(linkedScopes)) {
            return;
        }
        linkedScopes.forEach(scopeUuid -> {
            final ScopeResult scope = scopes.get(scopeUuid);
            if (Objects.nonNull(scope)) {
                synchronized (scope) {
                    normalizeScope(scope);
                    applyScopeMetadata(scope, testResult);
                }
                return;
            }
            final ScopeMetadata metadata = scopeMetadata.get(scopeUuid);
            if (Objects.nonNull(metadata)) {
                synchronized (metadata) {
                    metadata.applyTo(testResult);
                }
            }
        });
    }

    private void applyScopeMetadata(final ScopeResult scope, final TestResult testResult) {
        testResult.getLabels().addAll(scope.getLabels());
        testResult.getLinks().addAll(scope.getLinks());
        testResult.getParameters().addAll(scope.getParameters());
        if (Objects.isNull(testResult.getDescription())) {
            testResult.setDescription(scope.getDescription());
        }
        if (Objects.isNull(testResult.getDescriptionHtml())) {
            testResult.setDescriptionHtml(scope.getDescriptionHtml());
        }
    }

    private void linkExistingTests(final ScopeResult scope) {
        scope.getTests().forEach(childUuid -> linkTestToScope(scope.getUuid(), childUuid));
    }

    private void linkExistingChildren(final TestResultContainer container) {
        container.getChildren().forEach(childUuid -> linkTestToScope(container.getUuid(), childUuid));
    }

    private void linkTestToScope(final String scopeUuid, final String testUuid) {
        if (isEmpty(scopeUuid) || isEmpty(testUuid)) {
            return;
        }
        final Set<String> linkedScopes = testScopes.computeIfAbsent(testUuid, key -> new CopyOnWriteArraySet<>());
        final Set<String> visitedScopes = new HashSet<>();
        String current = scopeUuid;
        while (Objects.nonNull(current) && visitedScopes.add(current)) {
            linkedScopes.add(current);
            addTestToScope(current, testUuid);
            current = scopeParents.get(current);
        }
    }

    private void addTestToScope(final String scopeUuid, final String testUuid) {
        final ScopeResult scope = scopes.get(scopeUuid);
        if (Objects.isNull(scope)) {
            return;
        }
        synchronized (scope) {
            normalizeScope(scope);
            if (!scope.getTests().contains(testUuid)) {
                scope.getTests().add(testUuid);
            }
        }
    }

    private void addTestChildToScope(final String scopeUuid, final String testUuid) {
        final ScopeResult scope = scopes.get(scopeUuid);
        if (Objects.isNull(scope)) {
            return;
        }
        synchronized (scope) {
            normalizeScope(scope);
            if (!scope.getTestChildren().contains(testUuid)) {
                scope.getTestChildren().add(testUuid);
            }
        }
    }

    private void restoreContextAfterFixture(final String uuid) {
        final FixtureContext fixtureContext = fixtureContexts.remove(uuid);
        if (Objects.isNull(fixtureContext)) {
            threadContext.clear();
            return;
        }
        threadContext.set(fixtureContext.previousContext());
    }

    private boolean isEmpty(final String s) {
        return Objects.isNull(s) || s.isEmpty();
    }

    private record FixtureContext(String scopeUuid, ScopeFixtureType type, Deque<String> previousContext) {
    }

    private static final class ScopeMetadata {

        private final List<Label> labels = new ArrayList<>();
        private final List<Link> links = new ArrayList<>();
        private final List<Parameter> parameters = new ArrayList<>();
        private String description;
        private String descriptionHtml;

        private List<Label> getLabels() {
            return labels;
        }

        private List<Link> getLinks() {
            return links;
        }

        private List<Parameter> getParameters() {
            return parameters;
        }

        private void setDescription(final String description) {
            this.description = description;
        }

        private void setDescriptionHtml(final String descriptionHtml) {
            this.descriptionHtml = descriptionHtml;
        }

        private void applyTo(final TestResult testResult) {
            testResult.getLabels().addAll(labels);
            testResult.getLinks().addAll(links);
            testResult.getParameters().addAll(parameters);
            if (Objects.isNull(testResult.getDescription())) {
                testResult.setDescription(description);
            }
            if (Objects.isNull(testResult.getDescriptionHtml())) {
                testResult.setDescriptionHtml(descriptionHtml);
            }
        }
    }

    private static FileSystemResultsWriter getDefaultWriter() {
        final Properties properties = PropertiesUtils.loadAllureProperties();
        final String path = properties.getProperty("allure.results.directory", "allure-results");
        return new FileSystemResultsWriter(Paths.get(path));
    }

    private static LifecycleNotifier getDefaultNotifier() {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return new LifecycleNotifier(
                load(ContainerLifecycleListener.class, classLoader),
                load(TestLifecycleListener.class, classLoader),
                load(FixtureLifecycleListener.class, classLoader),
                load(StepLifecycleListener.class, classLoader)
        );
    }
}
