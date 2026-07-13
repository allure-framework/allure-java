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

import io.qameta.allure.internal.AllureExecutionContext;
import io.qameta.allure.internal.AllureThreadContext;
import io.qameta.allure.listener.ContainerLifecycleListener;
import io.qameta.allure.listener.FixtureLifecycleListener;
import io.qameta.allure.listener.LifecycleNotifier;
import io.qameta.allure.listener.StepLifecycleListener;
import io.qameta.allure.listener.TestLifecycleListener;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.FixtureResult;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.ScopeFixtureResult;
import io.qameta.allure.model.ScopeFixtureType;
import io.qameta.allure.model.ScopeResult;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;
import io.qameta.allure.model.WithAttachments;
import io.qameta.allure.model.WithMetadata;
import io.qameta.allure.model.WithSteps;
import io.qameta.allure.util.ExceptionUtils;
import io.qameta.allure.util.PropertiesUtils;
import io.qameta.allure.util.WellKnownFileExtensionsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static io.qameta.allure.AllureConstants.ATTACHMENT_FILE_SUFFIX;
import static io.qameta.allure.util.ResultsUtils.firstNonEmpty;
import static io.qameta.allure.util.ResultsUtils.getStatus;
import static io.qameta.allure.util.ResultsUtils.getStatusDetails;
import static io.qameta.allure.util.ResultsUtils.md5;
import static io.qameta.allure.util.ServiceLoaderUtils.load;

/**
 * The Allure lifecycle: one class, three method groups. The addressing mode of every method is readable from its
 * signature.
 *
 * <ul>
 *   <li><b>Manual core</b> — key-addressed methods. Every parent and owner is explicit; they touch no thread state
 *   and are safe from any thread. The exceptions are the start/stop transitions of tests and fixtures: starts bind
 *   the calling thread (the thread that calls start is by definition the executing thread), and stops unbind or
 *   restore only when the calling thread's root is the stopped key.</li>
 *   <li><b>Ambient group</b> — keyless overloads that resolve their target from the calling thread's binding.</li>
 *   <li><b>Thread group</b> — explicit binding control: {@link #setCurrent(AllureExternalKey)},
 *   {@link #clearCurrent()}, {@link #bind(AllureExternalKey)}, {@link #bindDetached(AllureExternalKey)}, and the
 *   current-key accessors.</li>
 * </ul>
 *
 * <p>Integration adapters model suite-level grouping through flat scopes using
 * {@link #registerScope(AllureExternalKey)}, {@link #addTestToScope(AllureExternalKey, AllureExternalKey)}, and
 * {@link #writeScope(AllureExternalKey)}.</p>
 */
@SuppressWarnings(
    {
            "PMD.AvoidSynchronizedStatement",
            "PMD.GodClass", "PMD.TooManyMethods", "ClassFanOutComplexity"}
)
public class AllureLifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureLifecycle.class);

    private static final String EXTERNAL_KEY = "external key";

    private static final String KEY_NOT_FOUND = "Could not {}: item with key {} not found";

    private static final String WRONG_ENTITY = "Could not {}: item with key {} is not the expected type";

    private static final String KEY_ALREADY_EXISTS = "Could not {}: item with key {} already exists";

    private static final String NO_CONTEXT_FOR_ATTACHMENT = "Could not add attachment: no test or fixture running";

    private static final String ADD_TEST_TO_SCOPE = "add test to scope";

    private static final String SCHEDULE_TEST = "schedule test";

    private static final String START_FIXTURE = "start fixture";

    private static final String START_STEP = "start step";

    private final AllureResultsWriter writer;

    private final AllureThreadContext threadContext;

    private final LifecycleNotifier notifier;

    private final Map<AllureExternalKey, Object> items = new ConcurrentHashMap<>();

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
        this.threadContext = new AllureThreadContext();
    }

    // ── Scopes ───────────────────────────────────────────────────────────────────────────────

    /**
     * Registers scope.
     *
     * @param key the external scope key
     */
    public void registerScope(final AllureExternalKey key) {
        Objects.requireNonNull(key, EXTERNAL_KEY);
        final ScopeResult scope = new ScopeResult()
                .setUuid(UUID.randomUUID().toString());
        if (Objects.nonNull(items.putIfAbsent(key, new ScopeItem(scope)))) {
            LOGGER.warn(KEY_ALREADY_EXISTS, "register scope", key);
        }
    }

    /**
     * Adds test to scope. The test is referenced by its runtime key, so it must still be live in storage; the
     * scope's metadata is merged into it when it stops.
     *
     * @param scopeKey the external scope key
     * @param testKey  the external test key
     */
    public void addTestToScope(final AllureExternalKey scopeKey, final AllureExternalKey testKey) {
        final ScopeItem scope = getItem(scopeKey, ScopeItem.class, ADD_TEST_TO_SCOPE);
        final TestItem test = getItem(testKey, TestItem.class, ADD_TEST_TO_SCOPE);
        if (Objects.isNull(scope) || Objects.isNull(test)) {
            return;
        }
        test.scopes().add(scopeKey);
        addTest(scope, test.result().getUuid());
    }

    /**
     * Adds test to scope, referencing the test by its model uuid instead of a runtime key. Use for tests that are
     * already written and released from storage — the normal case for a scope that is written after its children,
     * such as an after-method scope or a suite scope closing at run end.
     *
     * @param scopeKey the external scope key
     * @param testUuid the model uuid of the test
     */
    public void addTestToScope(final AllureExternalKey scopeKey, final String testUuid) {
        final ScopeItem scope = getItem(scopeKey, ScopeItem.class, ADD_TEST_TO_SCOPE);
        if (Objects.isNull(scope)) {
            return;
        }
        addTest(scope, testUuid);
    }

    /**
     * Writes scope.
     *
     * @param key the external scope key
     */
    public void writeScope(final AllureExternalKey key) {
        final ScopeItem scope = getItem(key, ScopeItem.class, "write scope");
        if (Objects.isNull(scope)) {
            return;
        }
        final TestResultContainer container;
        synchronized (scope) {
            container = toScopeContainer(scope.result());
        }
        // the scope may be written before its linked tests stop (for example TestNG per-method scopes are written
        // at test start) — drain its metadata into the still-live tests now, claiming the link so the merge at
        // stopTest cannot apply it twice
        items.values().forEach(item -> {
            if (item instanceof TestItem && ((TestItem) item).scopes().remove(key)) {
                synchronized (scope) {
                    mergeScopeMetadata(scope.result(), ((TestItem) item).result());
                }
            }
        });
        notifier.beforeContainerWrite(container);
        waitForFutures(scope.futures());
        writer.write(container);
        sweepOwnedSteps(key);
        items.remove(key);
        notifier.afterContainerWrite(container);
    }

    // ── Tests ────────────────────────────────────────────────────────────────────────────────

    /**
     * Schedules test with given key.
     *
     * @param key    the external test key
     * @param result the test to schedule
     */
    public void scheduleTest(final AllureExternalKey key, final TestResult result) {
        Objects.requireNonNull(key, EXTERNAL_KEY);
        if (items.containsKey(key)) {
            LOGGER.warn(KEY_ALREADY_EXISTS, SCHEDULE_TEST, key);
            return;
        }
        if (firstNonEmpty(result.getUuid()).isEmpty()) {
            result.setUuid(UUID.randomUUID().toString());
        }
        notifier.beforeTestSchedule(result);
        result.setStage(Stage.SCHEDULED);
        if (Objects.nonNull(items.putIfAbsent(key, new TestItem(result)))) {
            LOGGER.warn(KEY_ALREADY_EXISTS, SCHEDULE_TEST, key);
            return;
        }
        notifier.afterTestSchedule(result);
    }

    /**
     * Schedules test with given scopes.
     *
     * @param scopeKeys the external scope keys
     * @param key       the external test key
     * @param result    the test to schedule
     */
    public void scheduleTest(final Collection<AllureExternalKey> scopeKeys,
                             final AllureExternalKey key,
                             final TestResult result) {
        scheduleTest(key, result);
        scopeKeys.forEach(scopeKey -> addTestToScope(scopeKey, key));
    }

    /**
     * Starts test with given key and binds it as the calling thread's root. The test must be scheduled.
     *
     * @param key the external test key
     */
    public void startTest(final AllureExternalKey key) {
        final TestItem item = getItem(key, TestItem.class, "start test");
        if (Objects.isNull(item)) {
            return;
        }
        final TestResult testResult = item.result();
        if (!Stage.SCHEDULED.equals(testResult.getStage())) {
            LOGGER.warn("Could not start test: test with key {} is not scheduled", key);
            return;
        }
        threadContext.clear();
        notifier.beforeTestStart(testResult);
        testResult
                .setStage(Stage.RUNNING)
                .setStart(System.currentTimeMillis());
        threadContext.start(key);
        notifier.afterTestStart(testResult);
    }

    /**
     * Updates test by given key.
     *
     * @param key    the external test key
     * @param update the update function
     */
    public void updateTest(final AllureExternalKey key, final Consumer<TestResult> update) {
        final TestItem item = getItem(key, TestItem.class, "update test");
        if (Objects.isNull(item)) {
            return;
        }
        notifier.beforeTestUpdate(item.result());
        update.accept(item.result());
        notifier.afterTestUpdate(item.result());
    }

    /**
     * Updates current running test.
     *
     * @param update the update function.
     */
    public void updateTest(final Consumer<TestResult> update) {
        final Optional<AllureExternalKey> root = threadContext.getRoot();
        if (root.isEmpty()) {
            LOGGER.warn("Could not update test: no test running");
            return;
        }
        updateTest(root.get(), update);
    }

    /**
     * Stops test by given key. The test must be running; scope metadata is merged into the test here. If the test has
     * a test case id but no history id, a compatibility history id is generated from the test case id and the final
     * parameters. A history id supplied by a {@link TestLifecycleListener#beforeTestStop(TestResult)} listener is
     * preserved. Unbinds the calling thread only if the test is the calling thread's root.
     *
     * @param key the external test key
     */
    public void stopTest(final AllureExternalKey key) {
        final TestItem item = getItem(key, TestItem.class, "stop test");
        if (Objects.isNull(item)) {
            return;
        }
        final TestResult testResult = item.result();
        if (!Stage.RUNNING.equals(testResult.getStage())) {
            LOGGER.warn("Could not stop test: test with key {} is not running", key);
            return;
        }
        if (isCurrentRoot(key)) {
            closeOpenStages();
        }
        notifier.beforeTestStop(testResult);
        testResult
                .setStage(Stage.FINISHED)
                .setStop(System.currentTimeMillis());
        if (Objects.isNull(testResult.getParameters())) {
            testResult.setParameters(new ArrayList<>());
        }
        applyScopeMetadata(item);
        if (Objects.isNull(testResult.getHistoryId()) && Objects.nonNull(testResult.getTestCaseId())) {
            testResult.setHistoryId(calculateHistoryId(testResult.getTestCaseId(), testResult.getParameters()));
        }
        if (isCurrentRoot(key)) {
            threadContext.clear();
        }
        notifier.afterTestStop(testResult);
    }

    private static String calculateHistoryId(final String testCaseId, final List<Parameter> parameters) {
        final StringBuilder source = new StringBuilder(testCaseId);
        final Stream<Parameter> parameterStream = Objects.isNull(parameters) ? Stream.empty() : parameters.stream();
        parameterStream
                .filter(Objects::nonNull)
                .filter(parameter -> !Boolean.TRUE.equals(parameter.getExcluded()))
                .sorted(
                        Comparator.comparing((Parameter parameter) -> Objects.toString(parameter.getName(), ""))
                                .thenComparing(parameter -> Objects.toString(parameter.getValue(), ""))
                )
                .forEachOrdered(
                        parameter -> source
                                .append(Objects.toString(parameter.getName(), ""))
                                .append(Objects.toString(parameter.getValue(), ""))
                );
        return md5(source.toString());
    }

    /**
     * Writes test by given key. Waits for the test's pending async attachments before serializing, so the written
     * result file is a completion marker: everything it references exists.
     *
     * @param key the external test key
     */
    public void writeTest(final AllureExternalKey key) {
        final TestItem item = getItem(key, TestItem.class, "write test");
        if (Objects.isNull(item)) {
            return;
        }
        notifier.beforeTestWrite(item.result());
        waitForFutures(item.futures());
        writer.write(item.result());
        sweepOwnedSteps(key);
        items.remove(key);
        notifier.afterTestWrite(item.result());
    }

    // ── Fixtures ─────────────────────────────────────────────────────────────────────────────

    /**
     * Starts a new before fixture with given scope and binds it as the calling thread's root, saving the previous
     * binding for {@link #stopFixture(AllureExternalKey)} to restore.
     *
     * @param scopeKey   the external scope key
     * @param fixtureKey the external fixture key
     * @param result     the fixture
     */
    public void startBeforeFixture(final AllureExternalKey scopeKey, final AllureExternalKey fixtureKey,
                                   final FixtureResult result) {
        startFixture(scopeKey, fixtureKey, result, ScopeFixtureType.BEFORE);
    }

    /**
     * Starts a new after fixture with given scope and binds it as the calling thread's root, saving the previous
     * binding for {@link #stopFixture(AllureExternalKey)} to restore.
     *
     * @param scopeKey   the external scope key
     * @param fixtureKey the external fixture key
     * @param result     the fixture
     */
    public void startAfterFixture(final AllureExternalKey scopeKey, final AllureExternalKey fixtureKey,
                                  final FixtureResult result) {
        startFixture(scopeKey, fixtureKey, result, ScopeFixtureType.AFTER);
    }

    private void startFixture(final AllureExternalKey scopeKey, final AllureExternalKey key,
                              final FixtureResult result, final ScopeFixtureType type) {
        Objects.requireNonNull(key, EXTERNAL_KEY);
        final ScopeItem scope = getItem(scopeKey, ScopeItem.class, START_FIXTURE);
        if (Objects.isNull(scope)) {
            return;
        }
        if (items.containsKey(key)) {
            LOGGER.warn(KEY_ALREADY_EXISTS, START_FIXTURE, key);
            return;
        }
        synchronized (scope) {
            scope.result().getFixtures().add(
                    new ScopeFixtureResult()
                            .setUuid(UUID.randomUUID().toString())
                            .setValue(result)
                            .setType(type)
                            .setScopeUuid(scope.result().getUuid())
            );
        }
        notifier.beforeFixtureStart(result);
        final FixtureItem item = new FixtureItem(result, scopeKey, type, threadContext.copy());
        items.put(key, item);
        result.setStage(Stage.RUNNING);
        result.setStart(System.currentTimeMillis());
        threadContext.clear();
        threadContext.start(key);
        notifier.afterFixtureStart(result);
    }

    /**
     * Updates fixture by given key.
     *
     * @param key    the external fixture key
     * @param update the update function
     */
    public void updateFixture(final AllureExternalKey key, final Consumer<FixtureResult> update) {
        final FixtureItem item = getItem(key, FixtureItem.class, "update fixture");
        if (Objects.isNull(item)) {
            return;
        }
        notifier.beforeFixtureUpdate(item.result());
        update.accept(item.result());
        notifier.afterFixtureUpdate(item.result());
    }

    /**
     * Updates current running fixture.
     *
     * @param update the update function.
     */
    public void updateFixture(final Consumer<FixtureResult> update) {
        final Optional<AllureExternalKey> root = threadContext.getRoot();
        if (root.isEmpty()) {
            LOGGER.warn("Could not update fixture: no fixture running");
            return;
        }
        updateFixture(root.get(), update);
    }

    /**
     * Stops fixture by given key. Restores the binding saved at fixture start only if the fixture is the calling
     * thread's root.
     *
     * @param key the external fixture key
     */
    public void stopFixture(final AllureExternalKey key) {
        final FixtureItem item = getItem(key, FixtureItem.class, "stop fixture");
        if (Objects.isNull(item)) {
            return;
        }
        final FixtureResult fixture = item.result();
        if (isCurrentRoot(key)) {
            closeOpenStages();
        }
        notifier.beforeFixtureStop(fixture);
        fixture.setStage(Stage.FINISHED);
        fixture.setStop(System.currentTimeMillis());
        if (isCurrentRoot(key)) {
            threadContext.set(item.savedContext());
        }
        items.remove(key);
        notifier.afterFixtureStop(fixture);
    }

    // ── Steps ────────────────────────────────────────────────────────────────────────────────

    /**
     * Starts a new step as a child of the current executable and makes it current on the calling thread. Takes no
     * effect if no executable is running.
     *
     * @param result the step
     */
    public void startStep(final StepResult result) {
        startStep(AllureExternalKey.random(AllureLifecycle.class), result);
    }

    /**
     * Starts a new step as a child of the current executable and makes it current on the calling thread, using the
     * given key as the step's identity. The key lets callers address this step later (for example a
     * {@code StepContext} that must target this step even while a nested step is current). Takes no effect if no
     * executable is running.
     *
     * @param key    the external step key
     * @param result the step
     */
    public void startStep(final AllureExternalKey key, final StepResult result) {
        final Optional<AllureExternalKey> current = threadContext.getCurrentExecutable();
        if (current.isEmpty()) {
            LOGGER.warn("Could not start step: no test or fixture running");
            return;
        }
        startStep(current.get(), key, result, true);
    }

    /**
     * Starts a new step as a child of the specified parent. Pure manual linkage: the step is attached under the
     * parent and no thread state is touched, so this is safe to call from any thread. Stop it with
     * {@link #stopStep(AllureExternalKey)}.
     *
     * @param parentKey the external parent key
     * @param key       the external step key
     * @param result    the step
     */
    public void startStep(final AllureExternalKey parentKey, final AllureExternalKey key, final StepResult result) {
        startStep(parentKey, key, result, false);
    }

    private void startStep(final AllureExternalKey parentKey, final AllureExternalKey key,
                           final StepResult result, final boolean bind) {
        startStep(parentKey, key, result, bind, false);
    }

    private void startStep(final AllureExternalKey parentKey, final AllureExternalKey key,
                           final StepResult result, final boolean bind, final boolean stage) {
        Objects.requireNonNull(key, EXTERNAL_KEY);
        Objects.requireNonNull(parentKey, EXTERNAL_KEY);
        final Object parent = items.get(parentKey);
        if (Objects.isNull(parent)) {
            LOGGER.warn(KEY_NOT_FOUND, START_STEP, parentKey);
            return;
        }
        final Object parentModel = modelOf(parent);
        if (!(parentModel instanceof WithSteps)) {
            LOGGER.warn(WRONG_ENTITY, START_STEP, parentKey);
            return;
        }
        if (items.containsKey(key)) {
            LOGGER.warn(KEY_ALREADY_EXISTS, START_STEP, key);
            return;
        }

        notifier.beforeStepStart(result);
        result.setStage(Stage.RUNNING);
        result.setStart(System.currentTimeMillis());

        if (bind) {
            threadContext.start(key);
        }
        final AllureExecutionContext snapshot = bind
                ? threadContext.copy()
                : deriveSnapshot(parentKey, parent, key);
        items.put(key, new StepItem(result, writeOwnerOf(parentKey, parent), snapshot, stage));
        synchronized (parent) {
            ((WithSteps) parentModel).getSteps().add(result);
        }
        notifier.afterStepStart(result);
    }

    /**
     * Starts a stage — a lightweight phase marker rendered as a regular step. A stage has no explicit stop: it stays
     * open, collecting the steps and attachments that follow, until the next stage starts at the same level or the
     * enclosing step, test, or fixture ends. A stage started inside a step becomes a child of that step. A stage
     * with no status when it closes is marked passed.
     *
     * <p>Stages are an ambient-only concept: their lifetime is defined by the calling thread's binding, so there is
     * no key-addressed form. Takes no effect if no executable is running.</p>
     *
     * @param result the stage step, carrying its name
     */
    public void startStage(final StepResult result) {
        if (threadContext.getCurrentExecutable().isEmpty()) {
            LOGGER.warn("Could not start stage: no test or fixture running");
            return;
        }
        closeOpenStages();
        final Optional<AllureExternalKey> parent = threadContext.getCurrentExecutable();
        if (parent.isEmpty()) {
            return;
        }
        startStep(parent.get(), AllureExternalKey.random(AllureLifecycle.class), result, true, true);
    }

    /**
     * Closes consecutive open stages on top of the calling thread's stack. A stage with no status is marked passed.
     */
    private void closeOpenStages() {
        closeOpenStagesAbove(null);
    }

    /**
     * Closes consecutive open stages bound above the given step on the calling thread's stack, or all consecutive
     * top stages when the step is {@code null}. A stage with no status is marked passed.
     */
    private void closeOpenStagesAbove(final AllureExternalKey key) {
        while (true) {
            final Optional<AllureExternalKey> current = threadContext.getCurrentStep();
            if (current.isEmpty() || current.get().equals(key)) {
                return;
            }
            final Object item = items.get(current.get());
            if (!(item instanceof StepItem) || !((StepItem) item).stage()) {
                return;
            }
            if (Objects.isNull(((StepItem) item).result().getStatus())) {
                ((StepItem) item).result().setStatus(Status.PASSED);
            }
            stopStep(current.get(), true);
        }
    }

    /**
     * Updates step by specified key.
     *
     * @param key    the external step key
     * @param update the update function
     */
    public void updateStep(final AllureExternalKey key, final Consumer<StepResult> update) {
        final StepItem item = getItem(key, StepItem.class, "update step");
        if (Objects.isNull(item)) {
            return;
        }
        notifier.beforeStepUpdate(item.result());
        update.accept(item.result());
        notifier.afterStepUpdate(item.result());
    }

    /**
     * Updates the current running step. A stage cannot be updated: stages are addressed by nobody once started, so
     * when the current step is a stage this warns and does nothing — a caller finishing its own step must address
     * it by key.
     *
     * @param update the update function.
     */
    public void updateStep(final Consumer<StepResult> update) {
        final Optional<AllureExternalKey> current = threadContext.getCurrentStep();
        if (current.isEmpty()) {
            LOGGER.warn("Could not update step: no step running");
            return;
        }
        final Object item = items.get(current.get());
        if (item instanceof StepItem && ((StepItem) item).stage()) {
            LOGGER.warn("Could not update step: the current step is a stage");
            return;
        }
        updateStep(current.get(), update);
    }

    /**
     * Stops step by given key. Pure manual form with one thread-affine convenience: when the stopped step is bound
     * on the calling thread with open stages above it, those stages are closed first.
     *
     * @param key the external step key
     */
    public void stopStep(final AllureExternalKey key) {
        if (threadContext.getLocalKeys().contains(key)) {
            closeOpenStagesAbove(key);
        }
        stopStep(key, false);
    }

    /**
     * Stops the current running step and pops it from the calling thread. Open stages above it are closed first.
     */
    public void stopStep() {
        closeOpenStages();
        final Optional<AllureExternalKey> current = threadContext.getCurrentStep();
        if (current.isEmpty()) {
            LOGGER.warn("Could not stop step: no step running");
            return;
        }
        stopStep(current.get(), true);
    }

    private void stopStep(final AllureExternalKey key, final boolean unbind) {
        final StepItem item = getItem(key, StepItem.class, "stop step");
        if (Objects.isNull(item)) {
            return;
        }
        final StepResult step = item.result();
        notifier.beforeStepStop(step);
        step.setStage(Stage.FINISHED);
        step.setStop(System.currentTimeMillis());
        items.remove(key);
        if (unbind) {
            threadContext.stop();
        }
        notifier.afterStepStop(step);
    }

    /**
     * Logs an instant step — started and finished in one call — under the current executable. The step is bound as
     * current for the duration of its listener callbacks, so listeners observe it exactly like a regular step.
     * Takes no effect if no executable is running.
     *
     * @param result the step, carrying its name and status
     */
    public void logStep(final StepResult result) {
        final Optional<AllureExternalKey> current = threadContext.getCurrentExecutable();
        if (current.isEmpty()) {
            LOGGER.warn("Could not log step: no test or fixture running");
            return;
        }
        final AllureExternalKey key = AllureExternalKey.random(AllureLifecycle.class);
        startStep(current.get(), key, result, true);
        if (items.containsKey(key)) {
            stopStep(key, true);
        }
    }

    /**
     * Logs an instant step — started and finished in one call — under the specified parent. Pure manual linkage:
     * no thread state is touched, so this is safe to call from any thread.
     *
     * @param parentKey the external parent key
     * @param result    the step, carrying its name and status
     */
    public void logStep(final AllureExternalKey parentKey, final StepResult result) {
        final AllureExternalKey key = AllureExternalKey.random(AllureLifecycle.class);
        startStep(parentKey, key, result, false);
        if (items.containsKey(key)) {
            stopStep(key, false);
        }
    }

    // ── Attachments ──────────────────────────────────────────────────────────────────────────

    /**
     * Adds attachment to a running test, fixture, or step by key.
     *
     * @param key     the external executable key
     * @param name    the name of attachment
     * @param type    the content type of attachment
     * @param stream  attachment content
     * @param options the attachment options
     */
    public void addAttachment(final AllureExternalKey key, final String name, final String type,
                              final InputStream stream, final AttachmentOptions options) {
        addAttachmentLink(key, name, type, options)
                .ifPresent(source -> writer.write(source, stream));
    }

    /**
     * Adds attachment to the current test, fixture, or step if one is running.
     *
     * @param name    the name of attachment
     * @param type    the content type of attachment
     * @param stream  attachment content
     * @param options the attachment options
     */
    public void addAttachment(final String name, final String type,
                              final InputStream stream, final AttachmentOptions options) {
        final Optional<AllureExternalKey> current = threadContext.getCurrentExecutable();
        if (current.isEmpty()) {
            LOGGER.warn(NO_CONTEXT_FOR_ATTACHMENT);
            return;
        }
        addAttachment(current.get(), name, type, stream, options);
    }

    /**
     * Adds an async attachment to a running test, fixture, or step by key. The attachment content is awaited before
     * the owning test or scope is written.
     *
     * @param key     the external executable key
     * @param name    the name of attachment
     * @param type    the content type of attachment
     * @param body    the future stream that contains attachment content
     * @param options the attachment options
     * @return future completed when attachment content is written
     */
    public CompletableFuture<Void> addAttachmentAsync(final AllureExternalKey key, final String name,
                                                      final String type,
                                                      final CompletionStage<? extends InputStream> body,
                                                      final AttachmentOptions options) {
        return addAttachmentAsync(key, name, type, body, options, null);
    }

    /**
     * Adds an async attachment to the current test, fixture, or step if one is running. The attachment content is
     * awaited before the owning test or scope is written.
     *
     * @param name    the name of attachment
     * @param type    the content type of attachment
     * @param body    the future stream that contains attachment content
     * @param options the attachment options
     * @return future completed when attachment content is written
     */
    public CompletableFuture<Void> addAttachmentAsync(final String name, final String type,
                                                      final CompletionStage<? extends InputStream> body,
                                                      final AttachmentOptions options) {
        final Optional<AllureExternalKey> current = threadContext.getCurrentExecutable();
        if (current.isEmpty()) {
            LOGGER.warn(NO_CONTEXT_FOR_ATTACHMENT);
            return CompletableFuture.completedFuture(null);
        }
        return addAttachmentAsync(current.get(), name, type, body, options, null);
    }

    private CompletableFuture<Void> addAttachmentAsync(final AllureExternalKey key, final String name,
                                                       final String type,
                                                       final CompletionStage<? extends InputStream> body,
                                                       final AttachmentOptions options,
                                                       final BiConsumer<Void, Throwable> onComplete) {
        final Optional<String> source = addAttachmentLink(key, name, type, options);
        if (source.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        final String attachmentSource = source.get();
        CompletableFuture<Void> future = body
                .thenAccept(stream -> writer.write(attachmentSource, stream))
                .toCompletableFuture();
        if (Objects.nonNull(onComplete)) {
            future = future.whenComplete(onComplete);
        }
        final Optional<Set<CompletableFuture<?>>> futures = futuresOf(writeOwnerOf(key, items.get(key)));
        if (futures.isEmpty()) {
            LOGGER.warn("Could not track async attachment: no write owner found for key {}", key);
        } else {
            registerFuture(futures.get(), future);
        }
        return future;
    }

    /**
     * Adds an attachment wrapped in its own instant step under the current executable — the default representation
     * for user-facing attachments. Safe to call from listener callbacks: the wrapper step emits no further events.
     * Takes no effect if no executable is running.
     *
     * @param name    the name of attachment
     * @param type    the content type of attachment
     * @param content attachment content
     * @param options the attachment options
     */
    public void addAttachmentStep(final String name, final String type,
                                  final InputStream content, final AttachmentOptions options) {
        final Optional<AllureExternalKey> current = threadContext.getCurrentExecutable();
        if (current.isEmpty()) {
            LOGGER.warn(NO_CONTEXT_FOR_ATTACHMENT);
            return;
        }
        final AllureExternalKey key = AllureExternalKey.random(AllureLifecycle.class);
        startStep(current.get(), key, new StepResult().setName(attachmentStepName(name)), true);
        if (!items.containsKey(key)) {
            return;
        }
        try {
            addAttachment(key, name, type, content, options);
            updateStep(key, step -> step.setStatus(Status.PASSED));
        } catch (Throwable throwable) {
            updateStep(
                    key, step -> step
                            .setStatus(getStatus(throwable).orElse(Status.BROKEN))
                            .setStatusDetails(getStatusDetails(throwable).orElse(null))
            );
            throw ExceptionUtils.sneakyThrow(throwable);
        } finally {
            stopStep(key, true);
        }
    }

    /**
     * Adds an attachment wrapped in its own instant step under the specified parent — the default representation
     * for user-facing attachments. Pure manual linkage: no thread state is touched.
     *
     * @param parentKey the external parent key
     * @param name      the name of attachment
     * @param type      the content type of attachment
     * @param content   attachment content
     * @param options   the attachment options
     */
    public void addAttachmentStep(final AllureExternalKey parentKey, final String name, final String type,
                                  final InputStream content, final AttachmentOptions options) {
        final AllureExternalKey key = AllureExternalKey.random(AllureLifecycle.class);
        startStep(parentKey, key, new StepResult().setName(attachmentStepName(name)), false);
        if (!items.containsKey(key)) {
            return;
        }
        try {
            addAttachment(key, name, type, content, options);
            updateStep(key, step -> step.setStatus(Status.PASSED));
        } catch (Throwable throwable) {
            updateStep(
                    key, step -> step
                            .setStatus(getStatus(throwable).orElse(Status.BROKEN))
                            .setStatusDetails(getStatusDetails(throwable).orElse(null))
            );
            throw ExceptionUtils.sneakyThrow(throwable);
        } finally {
            stopStep(key, false);
        }
    }

    /**
     * Adds an async attachment wrapped in its own instant step under the current executable. The attachment content
     * is awaited before the owning test or scope is written; a failed body marks the step broken before the owner
     * is serialized. Takes no effect if no executable is running.
     *
     * @param name    the name of attachment
     * @param type    the content type of attachment
     * @param body    the future stream that contains attachment content
     * @param options the attachment options
     * @return future completed when attachment content is written
     */
    public CompletableFuture<Void> addAttachmentStepAsync(final String name, final String type,
                                                          final CompletionStage<? extends InputStream> body,
                                                          final AttachmentOptions options) {
        final Optional<AllureExternalKey> current = threadContext.getCurrentExecutable();
        if (current.isEmpty()) {
            LOGGER.warn(NO_CONTEXT_FOR_ATTACHMENT);
            return CompletableFuture.completedFuture(null);
        }
        final AllureExternalKey key = AllureExternalKey.random(AllureLifecycle.class);
        final StepResult step = new StepResult()
                .setName(attachmentStepName(name))
                .setStatus(Status.PASSED);
        startStep(current.get(), key, step, true);
        if (!items.containsKey(key)) {
            return CompletableFuture.completedFuture(null);
        }
        try {
            // the status update runs inside the tracked future, so a failed async attachment is
            // guaranteed to mark the step before the owning result is written
            return addAttachmentAsync(key, name, type, body, options, (result, throwable) -> {
                if (Objects.nonNull(throwable)) {
                    step.setStatus(getStatus(throwable).orElse(Status.BROKEN))
                            .setStatusDetails(getStatusDetails(throwable).orElse(null));
                }
            });
        } finally {
            stopStep(key, true);
        }
    }

    /**
     * Adds an async attachment wrapped in its own instant step under the specified parent. Pure manual linkage: no
     * thread state is touched. The attachment content is awaited before the owning test or scope is written; a
     * failed body marks the step broken before the owner is serialized.
     *
     * @param parentKey the external parent key
     * @param name      the name of attachment
     * @param type      the content type of attachment
     * @param body      the future stream that contains attachment content
     * @param options   the attachment options
     * @return future completed when attachment content is written
     */
    public CompletableFuture<Void> addAttachmentStepAsync(final AllureExternalKey parentKey, final String name,
                                                          final String type,
                                                          final CompletionStage<? extends InputStream> body,
                                                          final AttachmentOptions options) {
        final AllureExternalKey key = AllureExternalKey.random(AllureLifecycle.class);
        final StepResult step = new StepResult()
                .setName(attachmentStepName(name))
                .setStatus(Status.PASSED);
        startStep(parentKey, key, step, false);
        if (!items.containsKey(key)) {
            return CompletableFuture.completedFuture(null);
        }
        try {
            // the status update runs inside the tracked future, so a failed async attachment is
            // guaranteed to mark the step before the owning result is written
            return addAttachmentAsync(key, name, type, body, options, (result, throwable) -> {
                if (Objects.nonNull(throwable)) {
                    step.setStatus(getStatus(throwable).orElse(Status.BROKEN))
                            .setStatusDetails(getStatusDetails(throwable).orElse(null));
                }
            });
        } finally {
            stopStep(key, false);
        }
    }

    private static String attachmentStepName(final String name) {
        return firstNonEmpty(name).orElse("Attachment");
    }

    private Optional<String> addAttachmentLink(final AllureExternalKey key, final String name,
                                               final String type, final AttachmentOptions options) {
        Objects.requireNonNull(key, EXTERNAL_KEY);
        final Object item = items.get(key);
        final Object model = modelOf(item);
        if (!(model instanceof WithAttachments)) {
            LOGGER.warn(Objects.isNull(item) ? KEY_NOT_FOUND : WRONG_ENTITY, "add attachment", key);
            return Optional.empty();
        }
        final Attachment attachment = new Attachment()
                .setName(firstNonEmpty(name).orElse(null))
                .setType(firstNonEmpty(type).orElse(null))
                .setSource(createAttachmentSource(type, options));
        synchronized (item) {
            ((WithAttachments) model).getAttachments().add(attachment);
        }
        return Optional.of(attachment.getSource());
    }

    private static String createAttachmentSource(final String type, final AttachmentOptions options) {
        final String extension = Optional.ofNullable(options)
                .map(AttachmentOptions::getFileExtension)
                .orElseGet(() -> WellKnownFileExtensionsUtils.getExtensionByMimeType(type));
        return UUID.randomUUID() + ATTACHMENT_FILE_SUFFIX + normalizeFileExtension(extension);
    }

    private static String normalizeFileExtension(final String extension) {
        if (Objects.isNull(extension) || extension.isEmpty()) {
            return "";
        }
        return extension.charAt(0) == '.' ? extension : "." + extension;
    }

    private static void registerFuture(final Set<CompletableFuture<?>> futures, final CompletableFuture<?> future) {
        futures.add(future);
        future.whenComplete((result, throwable) -> futures.remove(future));
    }

    private static void waitForFutures(final Set<CompletableFuture<?>> futures) {
        if (futures.isEmpty()) {
            return;
        }
        final CompletableFuture<?>[] safeFutures = futures.stream()
                .map(future -> future.handle((result, throwable) -> null))
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(safeFutures).join();
    }

    // ── Metadata ─────────────────────────────────────────────────────────────────────────────

    /**
     * Applies a metadata update to the test-level target of the calling thread's root executable: in a test, the
     * test itself; in a before fixture, the fixture's scope — so the metadata propagates to every test of that
     * scope when it stops. Metadata written in an after fixture is dropped by design: its tests are already
     * stopped. Takes no effect if no executable is running.
     *
     * @param update the metadata update
     */
    public void updateTestMetadata(final Consumer<WithMetadata> update) {
        final Optional<AllureExternalKey> root = threadContext.getRoot();
        if (root.isEmpty()) {
            LOGGER.warn("Could not update test metadata: no test or fixture running");
            return;
        }
        final Object item = items.get(root.get());
        if (item instanceof TestItem) {
            updateTest(root.get(), update::accept);
            return;
        }
        if (item instanceof FixtureItem && ScopeFixtureType.BEFORE.equals(((FixtureItem) item).type())) {
            final ScopeItem scope = getItem(((FixtureItem) item).scopeKey(), ScopeItem.class, "update test metadata");
            if (Objects.nonNull(scope)) {
                synchronized (scope) {
                    update.accept(scope.result());
                    // the consumer is the only code that can break the lists-are-mutable invariant
                    normalizeScope(scope.result());
                }
            }
        }
        // after-fixture metadata is dropped by design — the scope's tests are already stopped
    }

    // ── Thread group ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the key of the calling thread's root executable — the running test or fixture, if any.
     *
     * @return current test or fixture key
     */
    public Optional<AllureExternalKey> getCurrentRootKey() {
        return threadContext.getRoot();
    }

    /**
     * Returns the key of the calling thread's current executable — the attach point for new steps and attachments: a
     * test, fixture, or step, if any.
     *
     * <p>The returned key is a manual-core identity that can be snapshotted and used later from any thread
     * (capture-now-apply-later).</p>
     *
     * @return current executable key
     */
    public Optional<AllureExternalKey> getCurrentExecutableKey() {
        return threadContext.getCurrentExecutable();
    }

    /**
     * Binds the test or fixture identified by the given key as the calling thread's root, replacing any current
     * binding. Use for callback-spanning context such as a test that is started and finished in separate framework
     * callbacks, possibly on different threads.
     *
     * @param key the external key to make current
     */
    public void setCurrent(final AllureExternalKey key) {
        Objects.requireNonNull(key, EXTERNAL_KEY);
        final Object item = items.get(key);
        if (!(item instanceof TestItem) && !(item instanceof FixtureItem)) {
            LOGGER.warn(Objects.isNull(item) ? KEY_NOT_FOUND : WRONG_ENTITY, "set current", key);
            return;
        }
        threadContext.clear();
        threadContext.start(key);
    }

    /**
     * Clears the calling thread's binding.
     */
    public void clearCurrent() {
        threadContext.clear();
    }

    /**
     * Binds the calling thread to the execution stream of the executable identified by the given key, continuing it.
     * The returned binding restores the previous context when closed.
     *
     * @param key the external key to bind from
     * @return the thread binding
     */
    public AllureThreadBinding bind(final AllureExternalKey key) {
        final AllureExecutionContext snapshot = snapshotOf(key, items.get(key));
        if (Objects.isNull(snapshot)) {
            LOGGER.warn(KEY_NOT_FOUND, "bind", key);
            threadContext.push(new AllureExecutionContext());
        } else {
            threadContext.push(snapshot.copy());
        }
        return new ThreadBinding(threadContext);
    }

    /**
     * Binds a detached child context anchored to the executable identified by the given key, with an empty local
     * stack. Use for independent worker-thread streams under the same executable. The returned binding restores the
     * previous context when closed.
     *
     * @param key the external key to anchor to
     * @return the thread binding
     */
    public AllureThreadBinding bindDetached(final AllureExternalKey key) {
        final AllureExecutionContext snapshot = snapshotOf(key, items.get(key));
        if (Objects.isNull(snapshot)) {
            LOGGER.warn(KEY_NOT_FOUND, "bind detached", key);
            threadContext.push(new AllureExecutionContext());
        } else {
            threadContext.push(snapshot.copy().child());
        }
        return new ThreadBinding(threadContext);
    }

    // ── Internals ────────────────────────────────────────────────────────────────────────────

    private <T> T getItem(final AllureExternalKey key, final Class<T> type, final String operation) {
        Objects.requireNonNull(key, EXTERNAL_KEY);
        final Object item = items.get(key);
        if (Objects.isNull(item)) {
            LOGGER.warn(KEY_NOT_FOUND, operation, key);
            return null;
        }
        if (!type.isInstance(item)) {
            LOGGER.warn(WRONG_ENTITY, operation, key);
            return null;
        }
        return type.cast(item);
    }

    private boolean isCurrentRoot(final AllureExternalKey key) {
        return threadContext.getRoot().filter(key::equals).isPresent();
    }

    private static Object modelOf(final Object item) {
        if (item instanceof TestItem) {
            return ((TestItem) item).result();
        }
        if (item instanceof FixtureItem) {
            return ((FixtureItem) item).result();
        }
        if (item instanceof StepItem) {
            return ((StepItem) item).result();
        }
        if (item instanceof ScopeItem) {
            return ((ScopeItem) item).result();
        }
        return null;
    }

    /**
     * Returns the execution context an item anchors: tests and fixtures always anchor {@code [key]} (start binds
     * them as the fresh root, so their snapshot is deterministic and never stored); steps carry the snapshot taken
     * at their start.
     */
    private static AllureExecutionContext snapshotOf(final AllureExternalKey key, final Object item) {
        if (item instanceof TestItem || item instanceof FixtureItem) {
            final AllureExecutionContext context = new AllureExecutionContext();
            context.start(key);
            return context;
        }
        if (item instanceof StepItem) {
            return ((StepItem) item).contextSnapshot();
        }
        return null;
    }

    private static AllureExternalKey writeOwnerOf(final AllureExternalKey key, final Object item) {
        if (item instanceof TestItem) {
            return key;
        }
        if (item instanceof FixtureItem) {
            return ((FixtureItem) item).scopeKey();
        }
        if (item instanceof StepItem) {
            return ((StepItem) item).writeOwnerKey();
        }
        if (item instanceof ScopeItem) {
            return key;
        }
        return null;
    }

    private Optional<Set<CompletableFuture<?>>> futuresOf(final AllureExternalKey ownerKey) {
        if (Objects.isNull(ownerKey)) {
            return Optional.empty();
        }
        final Object item = items.get(ownerKey);
        if (item instanceof TestItem) {
            return Optional.of(((TestItem) item).futures());
        }
        if (item instanceof ScopeItem) {
            return Optional.of(((ScopeItem) item).futures());
        }
        return Optional.empty();
    }

    private static AllureExecutionContext deriveSnapshot(final AllureExternalKey parentKey, final Object parentItem,
                                                         final AllureExternalKey key) {
        final AllureExecutionContext parentSnapshot = snapshotOf(parentKey, parentItem);
        final AllureExecutionContext snapshot = Objects.isNull(parentSnapshot)
                ? new AllureExecutionContext()
                : parentSnapshot.copy();
        snapshot.start(key);
        return snapshot;
    }

    private void sweepOwnedSteps(final AllureExternalKey ownerKey) {
        items.entrySet().removeIf(
                entry -> entry.getValue() instanceof StepItem
                        && ownerKey.equals(((StepItem) entry.getValue()).writeOwnerKey())
        );
    }

    private void addTest(final ScopeItem scope, final String testUuid) {
        if (firstNonEmpty(testUuid).isEmpty()) {
            return;
        }
        synchronized (scope) {
            if (!scope.result().getTests().contains(testUuid)) {
                scope.result().getTests().add(testUuid);
            }
        }
    }

    private void applyScopeMetadata(final TestItem item) {
        for (final AllureExternalKey scopeKey : List.copyOf(item.scopes())) {
            // claim the link before merging, so a concurrent writeScope drain cannot apply it twice
            if (!item.scopes().remove(scopeKey)) {
                continue;
            }
            final Object found = items.get(scopeKey);
            if (found instanceof ScopeItem) {
                final ScopeItem scope = (ScopeItem) found;
                synchronized (scope) {
                    mergeScopeMetadata(scope.result(), item.result());
                }
            }
        }
    }

    private static void mergeScopeMetadata(final ScopeResult scope, final TestResult testResult) {
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

    private static TestResultContainer toScopeContainer(final ScopeResult scope) {
        final TestResultContainer container = new TestResultContainer()
                .setUuid(scope.getUuid())
                .setChildren(new ArrayList<>(new LinkedHashSet<>(scope.getTests())));
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

    /**
     * Re-establishes the internal invariant that all scope lists are non-null and mutable. The model guarantees it
     * at construction and every internal mutation preserves it; the only code that can break it is the user-supplied
     * metadata consumer, so this runs once right after that consumer — never defensively anywhere else. Copying also
     * detaches any list alias the consumer may have retained.
     */
    private static void normalizeScope(final ScopeResult scope) {
        scope.setTests(new ArrayList<>(Objects.requireNonNullElse(scope.getTests(), List.of())));
        scope.setFixtures(new ArrayList<>(Objects.requireNonNullElse(scope.getFixtures(), List.of())));
        scope.setLabels(new ArrayList<>(Objects.requireNonNullElse(scope.getLabels(), List.of())));
        scope.setLinks(new ArrayList<>(Objects.requireNonNullElse(scope.getLinks(), List.of())));
        scope.setParameters(new ArrayList<>(Objects.requireNonNullElse(scope.getParameters(), List.of())));
    }

    // ── Items ────────────────────────────────────────────────────────────────────────────────

    /**
     * Internal item of a scheduled or running test: the result model, the scopes the test is linked to (their
     * metadata is merged into the test at stop), and the async attachment futures awaited before the test is written.
     */
    private record TestItem(TestResult result, Set<AllureExternalKey> scopes, Set<CompletableFuture<?>> futures) {

        private TestItem(final TestResult result) {
            this(result, ConcurrentHashMap.newKeySet(), ConcurrentHashMap.newKeySet());
        }
    }

    /**
     * Internal item of a running fixture: the result model, the owning scope, the fixture type, and the calling
     * thread's binding saved at start — restored by stop when the fixture is still the thread's root.
     */
    private record FixtureItem(FixtureResult result, AllureExternalKey scopeKey, ScopeFixtureType type,
            AllureExecutionContext savedContext) {
    }

    /**
     * Internal item of a running step: the result model, the write owner (the test or scope whose write awaits the
     * step's async attachments), the execution context captured at start (feeds {@code bind}/{@code bindDetached}),
     * and whether the step is a stage.
     */
    private record StepItem(StepResult result, AllureExternalKey writeOwnerKey,
            AllureExecutionContext contextSnapshot, boolean stage) {
    }

    /**
     * Internal item of a registered scope: the result model and the async attachment futures awaited before the
     * scope is written.
     */
    private record ScopeItem(ScopeResult result, Set<CompletableFuture<?>> futures) {

        private ScopeItem(final ScopeResult result) {
            this(result, ConcurrentHashMap.newKeySet());
        }
    }

    /**
     * Thread binding returned by {@code bind}/{@code bindDetached}: restores the previous context once, on the
     * first close.
     */
    private record ThreadBinding(AllureThreadContext threadContext, AtomicBoolean closed)
            implements
                AllureThreadBinding {

        private ThreadBinding(final AllureThreadContext threadContext) {
            this(threadContext, new AtomicBoolean());
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                threadContext.pop();
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
