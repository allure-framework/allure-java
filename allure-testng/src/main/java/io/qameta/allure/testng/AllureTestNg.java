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
package io.qameta.allure.testng;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureExternalKey;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.Flaky;
import io.qameta.allure.Muted;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.model.FixtureResult;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.testng.config.AllureTestNgConfig;
import io.qameta.allure.util.AnnotationUtils;
import io.qameta.allure.util.ObjectUtils;
import io.qameta.allure.util.ParameterUtils;
import io.qameta.allure.util.ResultsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IAttributes;
import org.testng.IClass;
import org.testng.IConfigurationListener;
import org.testng.IDataProviderListener;
import org.testng.IDataProviderMethod;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.IMethodInstance;
import org.testng.IMethodInterceptor;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestClass;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.annotations.Parameters;
import org.testng.internal.ConstructorOrMethod;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.qameta.allure.util.ResultsUtils.ALLURE_ID_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.bytesToHex;
import static io.qameta.allure.util.ResultsUtils.createFrameworkLabel;
import static io.qameta.allure.util.ResultsUtils.createHostLabel;
import static io.qameta.allure.util.ResultsUtils.createLanguageLabel;
import static io.qameta.allure.util.ResultsUtils.createPackageLabel;
import static io.qameta.allure.util.ResultsUtils.createParameter;
import static io.qameta.allure.util.ResultsUtils.createParentSuiteLabel;
import static io.qameta.allure.util.ResultsUtils.createSubSuiteLabel;
import static io.qameta.allure.util.ResultsUtils.createSuiteLabel;
import static io.qameta.allure.util.ResultsUtils.createTestClassLabel;
import static io.qameta.allure.util.ResultsUtils.createTestMethodLabel;
import static io.qameta.allure.util.ResultsUtils.createThreadLabel;
import static io.qameta.allure.util.ResultsUtils.createTitlePath;
import static io.qameta.allure.util.ResultsUtils.createTitlePathFromQualifiedClassName;
import static io.qameta.allure.util.ResultsUtils.firstNonEmpty;
import static io.qameta.allure.util.ResultsUtils.getMd5Digest;
import static io.qameta.allure.util.ResultsUtils.getProvidedLabels;
import static io.qameta.allure.util.ResultsUtils.getStatusDetails;
import static io.qameta.allure.util.ResultsUtils.processDescription;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Comparator.comparing;
import static java.util.Objects.nonNull;

/**
 * Reports TestNG execution to Allure.
 *
 * <p>Register this listener with TestNG to translate suites, test contexts, classes, configuration methods, data providers, and test methods into Allure scopes, fixtures, and test results. It also applies Allure test plan filtering when a plan is configured.</p>
 */
@SuppressWarnings(
    {
            "ClassDataAbstractionCoupling",
            "ClassFanOutComplexity",
            "PMD.GodClass",
            "PMD.TooManyMethods",
    }
)
public class AllureTestNg
        implements
            ISuiteListener,
            ITestListener,
            IInvokedMethodListener,
            IConfigurationListener,
            IMethodInterceptor,
            IDataProviderListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureTestNg.class);

    private static final String ALLURE_UUID = "ALLURE_UUID";
    private static final List<Class<?>> INJECTED_TYPES = Arrays.asList(
            ITestContext.class, ITestResult.class, XmlTest.class, Method.class, Object[].class
    );

    private static final boolean HAS_CUCUMBERJVM7_IN_CLASSPATH = isClassAvailableOnClasspath("io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm");

    /**
     * The test currently running on this thread: its allure uuid plus the testng result it belongs to. The result acts
     * as the invocation identity, so a terminal callback can tell an already-started test from one that was skipped
     * before its setup ever ran. Method-level after fixtures also read the uuid from here, since they run after the
     * terminal callback and have no access to the test result themselves.
     */
    private final ThreadLocal<CurrentTest> currentTest = new ThreadLocal<>();

    /**
     * Before-method fixture scopes awaiting their test. A before-method runs before the test is scheduled, so its scope
     * cannot list the test as a child yet; the scopes are held here and linked to the test, then written, once
     * {@link #onTestStart(ITestResult)} schedules it.
     */
    private final ThreadLocal<Deque<AllureExternalKey>> pendingBeforeMethodScopes = ThreadLocal.withInitial(ArrayDeque::new);

    /**
     * The after-method fixture scope in flight between its before/after invocation callbacks.
     */
    private final ThreadLocal<AllureExternalKey> currentAfterMethodScope = new ThreadLocal<>();

    /**
     * Store uuid for current executable item to catch steps and attachments.
     */
    private final ThreadLocal<String> currentExecutable = ThreadLocal
            .withInitial(() -> UUID.randomUUID().toString());

    /**
     * Store uuid for per-class scopes.
     */
    private final Map<ITestClass, String> classScopeUuidStorage = new ConcurrentHashMap<>();

    /**
     * Store uuid for data provider scopes.
     */
    private final Map<ITestNGMethod, String> dataProviderScopeUuidStorage = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final AllureLifecycle lifecycle;
    private final AllureTestNgTestFilter testFilter;

    private final AllureTestNgConfig config;

    /**
     * Package private constructor to allow custom configurations for unit tests.
     */
    AllureTestNg(final AllureLifecycle lifecycle,
                 final AllureTestNgTestFilter testFilter,
                 final AllureTestNgConfig config) {
        this.lifecycle = lifecycle;
        this.testFilter = testFilter;
        this.config = config;
    }

    public AllureTestNg(final AllureLifecycle lifecycle,
                        final AllureTestNgTestFilter testFilter) {
        this.lifecycle = lifecycle;
        this.testFilter = testFilter;
        this.config = AllureTestNgConfig.loadConfigProperties();
    }

    public AllureTestNg(final AllureLifecycle lifecycle) {
        this(lifecycle, new AllureTestNgTestFilter());
    }

    public AllureTestNg() {
        this(Allure.getLifecycle());
    }

    private static String safeExtractSuiteName(final ITestClass testClass) {
        final Optional<XmlTest> xmlTest = Optional.ofNullable(testClass.getXmlTest());
        return xmlTest.map(XmlTest::getSuite).map(XmlSuite::getName).orElse("Undefined suite");
    }

    private static String safeExtractTestTag(final ITestClass testClass) {
        final Optional<XmlTest> xmlTest = Optional.ofNullable(testClass.getXmlTest());
        return xmlTest.map(XmlTest::getName).orElse("Undefined testng tag");
    }

    private static String safeExtractTestClassName(final ITestClass testClass) {
        return firstNonEmpty(testClass.getTestName(), testClass.getName()).orElse("Undefined class name");
    }

    public AllureLifecycle getLifecycle() {
        return lifecycle;
    }

    private AllureExternalKey scopeKey(final String uuid) {
        return AllureExternalKey.of(AllureTestNg.class, "scope", uuid);
    }

    private AllureExternalKey testKey(final String uuid) {
        return AllureExternalKey.of(AllureTestNg.class, "test", uuid);
    }

    private AllureExternalKey fixtureKey(final String uuid) {
        return AllureExternalKey.of(AllureTestNg.class, "fixture", uuid);
    }

    @Override
    public List<IMethodInstance> intercept(final List<IMethodInstance> methods,
                                           final ITestContext context) {
        return testFilter.intercept(methods, context);
    }

    @Override
    public void onStart(final ISuite suite) {
        getLifecycle().registerScope(scopeKey(getUniqueUuid(suite)));
    }

    @Override
    public void onStart(final ITestContext context) {
        final String uuid = getUniqueUuid(context);
        getLifecycle().registerScope(scopeKey(uuid));

        Stream.of(context.getAllTestMethods())
                .map(ITestNGMethod::getTestClass)
                .distinct()
                .forEach(this::onBeforeClass);

        if (!config.isHideDisabledTests()) {
            context.getExcludedMethods().stream()
                    .filter(ITestNGMethod::isTest)
                    .filter(method -> !method.getEnabled())
                    .filter(testFilter::isSelected)
                    .forEach(method -> createFakeResult(context, method));
        }
    }

    protected void createFakeResult(final ITestContext context, final ITestNGMethod method) {
        final String uuid = UUID.randomUUID().toString();
        final String parentUuid = UUID.randomUUID().toString();
        startTest(context, method, method.getTestClass(), new Object[]{}, parentUuid, uuid);
        stopTest(uuid, null, null);
    }

    @Override
    public void onFinish(final ISuite suite) {
        final String uuid = getUniqueUuid(suite);
        getLifecycle().writeScope(scopeKey(uuid));

    }

    @Override
    public void onFinish(final ITestContext context) {
        final String uuid = getUniqueUuid(context);
        getLifecycle().writeScope(scopeKey(uuid));

        Stream.of(context.getAllTestMethods())
                .map(ITestNGMethod::getTestClass)
                .distinct()
                .forEach(this::onAfterClass);
    }

    public void onBeforeClass(final ITestClass testClass) {
        final String uuid = UUID.randomUUID().toString();
        getLifecycle().registerScope(scopeKey(uuid));
        setClassScope(testClass, uuid);
    }

    public void onAfterClass(final ITestClass testClass) {
        getClassScope(testClass).ifPresent(uuid -> {
            getLifecycle().writeScope(scopeKey(uuid));
        });
        dataProviderScopeUuidStorage.entrySet().removeIf(entry -> {
            if (entry.getKey().getTestClass().equals(testClass)) {
                getLifecycle().writeScope(scopeKey(entry.getValue()));
                return true;
            }
            return false;
        });
    }

    @Override
    public void onTestStart(final ITestResult testResult) {
        if (shouldSkipReportingFor(testResult)) {
            return;
        }

        final String uuid = UUID.randomUUID().toString();
        currentTest.set(new CurrentTest(testResult, uuid));
        final String parentUuid = getUniqueUuid(testResult.getTestContext());

        startTest(testResult, parentUuid, uuid);

        linkPendingBeforeMethodScopes(testKey(uuid));

        Optional.of(testResult)
                .map(ITestResult::getMethod)
                .map(ITestNGMethod::getTestClass)
                .ifPresent(clazz -> addTestToClassScope(clazz, uuid));

        Optional.of(testResult)
                .map(ITestResult::getMethod)
                .ifPresent(method -> addTestToDataProviderScope(method, uuid));
    }

    private void linkPendingBeforeMethodScopes(final AllureExternalKey testKey) {
        final Deque<AllureExternalKey> pending = pendingBeforeMethodScopes.get();
        while (!pending.isEmpty()) {
            final AllureExternalKey scopeKey = pending.poll();
            getLifecycle().addTestToScope(scopeKey, testKey);
            getLifecycle().writeScope(scopeKey);
        }
    }

    @SuppressWarnings("BooleanExpressionComplexity")
    protected boolean shouldSkipReportingFor(final ITestResult testResult) {
        final String[] groups = Optional.of(testResult)
                .map(ITestResult::getMethod)
                .map(ITestNGMethod::getGroups)
                .orElseGet(() -> new String[]{});

        if (groups.length == 0) {
            return false;
        }

        final Set<String> groupsSet = new HashSet<>(Arrays.asList(groups));
        return HAS_CUCUMBERJVM7_IN_CLASSPATH && groupsSet.contains("cucumber");
    }

    protected void startTest(final ITestResult testResult,
                             final String parentUuid,
                             final String uuid) {
        startTest(
                testResult.getTestContext(),
                testResult.getMethod(),
                testResult.getTestClass(),
                testResult.getParameters(),
                parentUuid,
                uuid
        );
    }

    @SuppressWarnings({"Indentation"})
    protected void startTest(final ITestContext context,
                             final ITestNGMethod method,
                             final IClass iClass,
                             final Object[] params,
                             final String parentUuid,
                             final String uuid) {
        final ITestClass testClass = method.getTestClass();
        final List<Label> labels = new ArrayList<>();
        labels.addAll(getProvidedLabels());
        labels.addAll(
                Arrays.asList(
                        //Packages grouping
                        createPackageLabel(testClass.getName()),
                        createTestClassLabel(testClass.getName()),
                        createTestMethodLabel(method.getMethodName()),

                        //xUnit grouping
                        createParentSuiteLabel(safeExtractSuiteName(testClass)),
                        createSuiteLabel(safeExtractTestTag(testClass)),
                        createSubSuiteLabel(safeExtractTestClassName(testClass)),

                        //Timeline grouping
                        createHostLabel(),
                        createThreadLabel(),

                        createFrameworkLabel("testng"),
                        createLanguageLabel("java")
                )
        );
        labels.addAll(getLabels(method, iClass));
        final List<Parameter> parameters = getParameters(context, method, params);
        final TestResult result = new TestResult()
                .setUuid(uuid)
                .setHistoryId(getHistoryId(method, parameters))
                .setName(getMethodName(method))
                .setFullName(getQualifiedName(method))
                .setTitlePath(getTitlePath(testClass))
                .setStatusDetails(
                        new StatusDetails()
                                .setFlaky(isFlaky(method, iClass))
                                .setMuted(isMuted(method, iClass))
                )
                .setParameters(parameters)
                .setLinks(getLinks(method, iClass))
                .setLabels(labels);

        processDescription(
                getClass().getClassLoader(),
                method.getConstructorOrMethod().getMethod(),
                result::setDescription,
                result::setDescriptionHtml
        );

        final AllureExternalKey testCaseKey = testKey(uuid);
        getLifecycle().scheduleTest(
                List.of(scopeKey(parentUuid), scopeKey(getUniqueUuid(context.getSuite()))),
                testCaseKey,
                result
        );
        getLifecycle().startTest(testCaseKey);
    }

    private List<String> getTitlePath(final ITestClass testClass) {
        final List<String> result = new ArrayList<>(
                createTitlePath(
                        safeExtractSuiteName(testClass),
                        safeExtractTestTag(testClass)
                )
        );
        result.addAll(createTitlePathFromQualifiedClassName(testClass.getName()));
        return result;
    }

    @Override
    public void onTestSuccess(final ITestResult testResult) {
        if (shouldSkipReportingFor(testResult)) {
            return;
        }

        final AllureExternalKey testCaseKey = testKey(currentTest.get().uuid());
        getLifecycle().updateTest(testCaseKey, setStatus(Status.PASSED));
        getLifecycle().stopTest(testCaseKey);
        getLifecycle().writeTest(testCaseKey);
    }

    @Override
    public void onTestFailure(final ITestResult result) {
        if (shouldSkipReportingFor(result)) {
            return;
        }

        final String uuid = ensureTestStarted(result);
        final Throwable throwable = result.getThrowable();
        final Status status = getStatus(throwable);
        stopTest(uuid, throwable, status);
    }

    protected void stopTest(final String uuid, final Throwable throwable, final Status status) {
        final StatusDetails details = getStatusDetails(throwable).orElse(null);
        final AllureExternalKey testCaseKey = testKey(uuid);
        getLifecycle().updateTest(testCaseKey, setStatus(status, details));
        getLifecycle().stopTest(testCaseKey);
        getLifecycle().writeTest(testCaseKey);
    }

    @Override
    public void onTestSkipped(final ITestResult result) {
        if (shouldSkipReportingFor(result)) {
            return;
        }

        final String uuid = ensureTestStarted(result);
        stopTest(uuid, result.getThrowable(), Status.SKIPPED);
    }

    /**
     * Returns the allure uuid of the test for the given result, starting it on the fly when the test is reaching a
     * terminal callback without ever having been started (for example a test skipped because a dependency failed, so
     * {@link #onTestStart(ITestResult)} never fired).
     */
    private String ensureTestStarted(final ITestResult result) {
        final CurrentTest running = currentTest.get();
        if (Objects.isNull(running) || running.source() != result) {
            onTestStart(result);
        }
        return currentTest.get().uuid();
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(final ITestResult result) {
        //do nothing
    }

    @Override
    public void beforeInvocation(final IInvokedMethod method, final ITestResult testResult) {
        final ITestNGMethod testMethod = method.getTestMethod();
        final ITestContext context = testResult.getTestContext();
        if (isSupportedConfigurationFixture(testMethod)) {
            ifSuiteFixtureStarted(context.getSuite(), testMethod);
            ifTestFixtureStarted(context, testMethod);
            ifClassFixtureStarted(testMethod);
            ifMethodFixtureStarted(testMethod);
        }
    }

    private void ifSuiteFixtureStarted(final ISuite suite, final ITestNGMethod testMethod) {
        if (testMethod.isBeforeSuiteConfiguration()) {
            startBefore(getUniqueUuid(suite), testMethod);
        }
        if (testMethod.isAfterSuiteConfiguration()) {
            startAfter(getUniqueUuid(suite), testMethod);
        }
    }

    private void ifClassFixtureStarted(final ITestNGMethod testMethod) {
        if (testMethod.isBeforeClassConfiguration()) {
            getClassScope(testMethod.getTestClass())
                    .ifPresent(parentUuid -> startBefore(parentUuid, testMethod));
        }
        if (testMethod.isAfterClassConfiguration()) {
            getClassScope(testMethod.getTestClass())
                    .ifPresent(parentUuid -> startAfter(parentUuid, testMethod));
        }
    }

    private void ifTestFixtureStarted(final ITestContext context, final ITestNGMethod testMethod) {
        if (testMethod.isBeforeTestConfiguration()) {
            startBefore(getUniqueUuid(context), testMethod);
        }
        if (testMethod.isAfterTestConfiguration()) {
            startAfter(getUniqueUuid(context), testMethod);
        }
    }

    private void startBefore(final String parentUuid, final ITestNGMethod method) {
        final String uuid = currentExecutable.get();
        getLifecycle().startBeforeFixture(scopeKey(parentUuid), fixtureKey(uuid), getFixtureResult(method));
    }

    private void startAfter(final String parentUuid, final ITestNGMethod method) {
        final String uuid = currentExecutable.get();
        getLifecycle().startAfterFixture(scopeKey(parentUuid), fixtureKey(uuid), getFixtureResult(method));
    }

    private void ifMethodFixtureStarted(final ITestNGMethod testMethod) {
        final FixtureResult fixture = getFixtureResult(testMethod);
        final String fixtureUuid = currentExecutable.get();
        final AllureExternalKey scopeKey = scopeKey(UUID.randomUUID().toString());
        getLifecycle().registerScope(scopeKey);

        if (testMethod.isBeforeMethodConfiguration()) {
            getLifecycle().startBeforeFixture(scopeKey, fixtureKey(fixtureUuid), fixture);
            // the test is not scheduled yet, so link the scope to it and write it once onTestStart fires
            pendingBeforeMethodScopes.get().add(scopeKey);
        }

        if (testMethod.isAfterMethodConfiguration()) {
            getLifecycle().startAfterFixture(scopeKey, fixtureKey(fixtureUuid), fixture);
            // the test has already finished and been written by now, so it is gone from storage and can only be
            // linked by its known uuid
            final CurrentTest running = currentTest.get();
            if (nonNull(running)) {
                getLifecycle().addTestToScope(scopeKey, running.uuid());
            }
            currentAfterMethodScope.set(scopeKey);
        }
    }

    private String getQualifiedName(final ITestNGMethod method) {
        return method.getRealClass().getName() + "." + method.getMethodName();
    }

    private FixtureResult getFixtureResult(final ITestNGMethod method) {
        final FixtureResult fixtureResult = new FixtureResult()
                .setName(getMethodName(method))
                .setStart(System.currentTimeMillis())
                .setDescription(method.getDescription())
                .setStage(Stage.RUNNING);

        processDescription(
                getClass().getClassLoader(),
                method.getConstructorOrMethod().getMethod(),
                fixtureResult::setDescription,
                fixtureResult::setDescriptionHtml
        );
        return fixtureResult;
    }

    @Override
    public void afterInvocation(final IInvokedMethod method, final ITestResult testResult) {
        final ITestNGMethod testMethod = method.getTestMethod();
        if (isSupportedConfigurationFixture(testMethod)) {
            final String executableUuid = currentExecutable.get();
            currentExecutable.remove();
            final AllureExternalKey fixtureCaseKey = fixtureKey(executableUuid);
            if (testResult.isSuccess()) {
                getLifecycle().updateFixture(fixtureCaseKey, result -> result.setStatus(Status.PASSED));
            } else {
                getLifecycle().updateFixture(
                        fixtureCaseKey, result -> result
                                .setStatus(getStatus(testResult.getThrowable()))
                                .setStatusDetails(getStatusDetails(testResult.getThrowable()).orElse(null))
                );
            }
            getLifecycle().stopFixture(fixtureCaseKey);

            // before-method scopes are written later, at onTestStart, once the test they link to exists
            if (testMethod.isAfterMethodConfiguration()) {
                final AllureExternalKey scopeKey = currentAfterMethodScope.get();
                currentAfterMethodScope.remove();
                if (nonNull(scopeKey)) {
                    getLifecycle().writeScope(scopeKey);
                }
            }
        }
    }

    @Override
    public void onConfigurationSuccess(final ITestResult itr) {
        //do nothing
    }

    @Override
    public void onConfigurationFailure(final ITestResult itr) {
        if (config.isHideConfigurationFailures()) {
            return; //do nothing
        }

        final String uuid = UUID.randomUUID().toString();
        final String parentUuid = UUID.randomUUID().toString();

        startTest(itr, parentUuid, uuid);

        linkTestToScope(getUniqueUuid(itr.getTestContext()), uuid);
        linkTestToScope(getUniqueUuid(itr.getTestContext().getSuite()), uuid);
        addTestToClassScope(itr.getMethod().getTestClass(), uuid);
        // results created for configuration failure should not be considered as test cases.
        getLifecycle().updateTest(
                testKey(uuid),
                tr -> tr.getLabels().add(
                        new Label().setName(ALLURE_ID_LABEL_NAME).setValue("-1")
                )
        );

        stopTest(uuid, itr.getThrowable(), getStatus(itr.getThrowable()));
    }

    @Override
    public void onConfigurationSkip(final ITestResult itr) {
        //do nothing
    }

    @Override
    public void beforeDataProviderExecution(final IDataProviderMethod dataProviderMethod,
                                            final ITestNGMethod method,
                                            final ITestContext iTestContext) {
        currentExecutable.remove();
        final String scopeUuid = dataProviderScopeUuidStorage.computeIfAbsent(
                method,
                key -> {
                    final String uuid = UUID.randomUUID().toString();
                    getLifecycle().registerScope(scopeKey(uuid));
                    return uuid;
                }
        );

        final String uuid = currentExecutable.get();
        final FixtureResult result = new FixtureResult()
                .setName(dataProviderMethod.getMethod().getName())
                .setStage(Stage.RUNNING);

        processDescription(
                getClass().getClassLoader(),
                dataProviderMethod.getMethod(),
                result::setDescription,
                result::setDescriptionHtml
        );

        getLifecycle().startBeforeFixture(scopeKey(scopeUuid), fixtureKey(uuid), result);
    }

    @Override
    public void afterDataProviderExecution(final IDataProviderMethod dataProviderMethod,
                                           final ITestNGMethod method,
                                           final ITestContext iTestContext) {
        final String uuid = currentExecutable.get();
        final AllureExternalKey fixtureCaseKey = fixtureKey(uuid);
        getLifecycle().updateFixture(fixtureCaseKey, result -> {
            if (result.getStatus() == null) {
                result.setStatus(Status.PASSED);
            }
        });
        getLifecycle().stopFixture(fixtureCaseKey);
        currentExecutable.remove();
    }

    @Override
    public void onDataProviderFailure(final ITestNGMethod method,
                                      final ITestContext ctx,
                                      final RuntimeException t) {
        final String uuid = currentExecutable.get();
        final AllureExternalKey fixtureCaseKey = fixtureKey(uuid);
        getLifecycle().updateFixture(
                fixtureCaseKey, result -> result
                        .setStatus(getStatus(t))
                        .setStatusDetails(getStatusDetails(t).orElse(null))
        );
        getLifecycle().stopFixture(fixtureCaseKey);
        currentExecutable.remove();
    }

    protected String getHistoryId(final ITestNGMethod method, final List<Parameter> parameters) {
        final MessageDigest digest = getMd5Digest();
        final String testClassName = method.getTestClass().getName();
        final String methodName = method.getMethodName();
        digest.update(testClassName.getBytes(UTF_8));
        digest.update(methodName.getBytes(UTF_8));
        parameters.stream()
                .sorted(comparing(Parameter::getName).thenComparing(Parameter::getValue))
                .forEachOrdered(parameter -> {
                    digest.update(parameter.getName().getBytes(UTF_8));
                    digest.update(parameter.getValue().getBytes(UTF_8));
                });
        final byte[] bytes = digest.digest();
        return bytesToHex(bytes);
    }

    protected Status getStatus(final Throwable throwable) {
        return ResultsUtils.getStatus(throwable).orElse(Status.BROKEN);
    }

    @SuppressWarnings("BooleanExpressionComplexity")
    private boolean isSupportedConfigurationFixture(final ITestNGMethod testMethod) {
        return testMethod.isBeforeMethodConfiguration() || testMethod.isAfterMethodConfiguration()
                || testMethod.isBeforeTestConfiguration() || testMethod.isAfterTestConfiguration()
                || testMethod.isBeforeClassConfiguration() || testMethod.isAfterClassConfiguration()
                || testMethod.isBeforeSuiteConfiguration() || testMethod.isAfterSuiteConfiguration();
    }

    private List<Label> getLabels(final ITestNGMethod method, final IClass iClass) {
        final List<Label> labels = new ArrayList<>();
        getMethod(method)
                .map(AnnotationUtils::getLabels)
                .ifPresent(labels::addAll);
        getClass(iClass)
                .map(AnnotationUtils::getLabels)
                .ifPresent(labels::addAll);

        getMethod(method)
                .map(this::getSeverity)
                .filter(Optional::isPresent)
                .orElse(getClass(iClass).flatMap(this::getSeverity))
                .map(ResultsUtils::createSeverityLabel)
                .ifPresent(labels::add);
        return labels;
    }

    private Optional<SeverityLevel> getSeverity(final AnnotatedElement annotatedElement) {
        return Stream.of(annotatedElement.getAnnotationsByType(Severity.class))
                .map(Severity::value)
                .findAny();
    }

    private List<Link> getLinks(final ITestNGMethod method, final IClass iClass) {
        final List<Link> links = new ArrayList<>();
        getMethod(method)
                .map(AnnotationUtils::getLinks)
                .ifPresent(links::addAll);
        getClass(iClass)
                .map(AnnotationUtils::getLinks)
                .ifPresent(links::addAll);
        return links;
    }

    private boolean isFlaky(final ITestNGMethod method, final IClass iClass) {
        final boolean flakyMethod = getMethod(method)
                .map(m -> m.isAnnotationPresent(Flaky.class))
                .orElse(false);
        final boolean flakyClass = getClass(iClass)
                .map(clazz -> clazz.isAnnotationPresent(Flaky.class))
                .orElse(false);
        return flakyMethod || flakyClass;
    }

    private boolean isMuted(final ITestNGMethod method, final IClass iClass) {
        final boolean mutedMethod = getMethod(method)
                .map(m -> m.isAnnotationPresent(Muted.class))
                .orElse(false);
        final boolean mutedClass = getClass(iClass)
                .map(clazz -> clazz.isAnnotationPresent(Muted.class))
                .orElse(false);
        return mutedMethod || mutedClass;
    }

    private Optional<Method> getMethod(final ITestNGMethod method) {
        return Optional.ofNullable(method)
                .map(ITestNGMethod::getConstructorOrMethod)
                .map(ConstructorOrMethod::getMethod);
    }

    private Optional<Class<?>> getClass(final IClass iClass) {
        return Optional.ofNullable(iClass)
                .map(IClass::getRealClass);
    }

    /**
     * Returns the unique id for given results item.
     */
    private String getUniqueUuid(final IAttributes suite) {
        if (Objects.isNull(suite.getAttribute(ALLURE_UUID))) {
            suite.setAttribute(ALLURE_UUID, UUID.randomUUID().toString());
        }
        return Objects.toString(suite.getAttribute(ALLURE_UUID));
    }

    @SuppressWarnings({"PMD.AvoidAccessibilityAlteration", "PMD.CognitiveComplexity"})
    private List<Parameter> getParameters(final ITestContext context,
                                          final ITestNGMethod method,
                                          final Object... parameters) {
        final Map<String, Parameter> result = new HashMap<>();
        context.getCurrentXmlTest().getAllParameters()
                .forEach((name, value) -> result.put(name, createParameter(name, value)));
        final Object instance = method.getInstance();
        if (nonNull(instance)) {
            Stream.of(instance.getClass().getDeclaredFields())
                    .filter(field -> field.isAnnotationPresent(TestInstanceParameter.class))
                    .forEach(field -> {
                        final String name = Optional.ofNullable(field.getAnnotation(TestInstanceParameter.class))
                                .map(TestInstanceParameter::value)
                                .filter(s -> !s.isEmpty())
                                .orElseGet(field::getName);
                        try {
                            field.setAccessible(true);
                            final String value = ObjectUtils.toString(field.get(instance));
                            result.put(name, createParameter(name, value));
                        } catch (IllegalAccessException e) {
                            LOGGER.debug("Could not access field value");
                        }
                    });
        }

        getMethod(method).ifPresent(m -> {
            final Class<?>[] parameterTypes = m.getParameterTypes();

            if (parameterTypes.length != parameters.length) {
                return;
            }

            final String[] providedNames = Optional.ofNullable(m.getAnnotation(Parameters.class))
                    .map(Parameters::value)
                    .orElse(new String[]{});

            final java.lang.reflect.Parameter[] reflectionParameters = m.getParameters();

            int skippedCount = 0;
            for (int i = 0; i < parameterTypes.length; i++) {
                final Class<?> parameterType = parameterTypes[i];
                if (INJECTED_TYPES.contains(parameterType)) {
                    skippedCount++;
                    continue;
                }

                final int indexFromAnnotation = i - skippedCount;
                final String defaultName = indexFromAnnotation < providedNames.length
                        ? providedNames[indexFromAnnotation]
                        : reflectionParameters[i].getName();
                final Parameter parameter = ParameterUtils.createParameter(
                        reflectionParameters[i],
                        parameters[i],
                        defaultName
                );
                if (nonNull(parameter.getName())) {
                    result.remove(defaultName);
                    result.put(parameter.getName(), parameter);
                }
            }

        });

        return result.values().stream()
                .collect(Collectors.toList());
    }

    private String getMethodName(final ITestNGMethod method) {
        return firstNonEmpty(
                method.getDescription(),
                method.getMethodName(),
                getQualifiedName(method)
        ).orElse("Unknown");
    }

    @SuppressWarnings("SameParameterValue")
    private Consumer<TestResult> setStatus(final Status status) {
        return result -> result.setStatus(status);
    }

    private Consumer<TestResult> setStatus(final Status status, final StatusDetails details) {
        return result -> {
            result.setStatus(status);
            if (nonNull(details)) {
                result.getStatusDetails().setTrace(details.getTrace());
                result.getStatusDetails().setMessage(details.getMessage());
                result.getStatusDetails().setActual(details.getActual());
                result.getStatusDetails().setExpected(details.getExpected());
            }
        };
    }

    private void addTestToDataProviderScope(final ITestNGMethod method, final String childUuid) {
        this.linkTestToScope(dataProviderScopeUuidStorage.get(method), childUuid);
    }

    private void addTestToClassScope(final ITestClass clazz, final String childUuid) {
        this.linkTestToScope(classScopeUuidStorage.get(clazz), childUuid);
    }

    private void linkTestToScope(final String scopeUuid, final String childUuid) {
        lock.writeLock().lock();
        try {
            if (nonNull(scopeUuid)) {
                getLifecycle().addTestToScope(scopeKey(scopeUuid), testKey(childUuid));
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private Optional<String> getClassScope(final ITestClass clazz) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(classScopeUuidStorage.get(clazz));
        } finally {
            lock.readLock().unlock();
        }
    }

    private void setClassScope(final ITestClass clazz, final String uuid) {
        lock.writeLock().lock();
        try {
            classScopeUuidStorage.put(clazz, uuid);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private static boolean isClassAvailableOnClasspath(final String clazz) {
        try {
            AllureTestNg.class.getClassLoader().loadClass(clazz);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * The test currently running on a thread: the testng result it belongs to (its invocation identity) and the allure
     * uuid assigned to it.
     */
    private record CurrentTest(ITestResult source, String uuid) {
    }

}
