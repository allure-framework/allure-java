package io.qameta.allure.junitplatform;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.util.ResultsUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.qameta.allure.model.Status.FAILED;
import static io.qameta.allure.model.Status.PASSED;
import static io.qameta.allure.model.Status.SKIPPED;
import static io.qameta.allure.util.ResultsUtils.createFrameworkLabel;
import static io.qameta.allure.util.ResultsUtils.createHostLabel;
import static io.qameta.allure.util.ResultsUtils.createLanguageLabel;
import static io.qameta.allure.util.ResultsUtils.createPackageLabel;
import static io.qameta.allure.util.ResultsUtils.createSuiteLabel;
import static io.qameta.allure.util.ResultsUtils.createTestClassLabel;
import static io.qameta.allure.util.ResultsUtils.createTestMethodLabel;
import static io.qameta.allure.util.ResultsUtils.createThreadLabel;
import static io.qameta.allure.util.ResultsUtils.getMd5Digest;
import static io.qameta.allure.util.ResultsUtils.getProvidedLabels;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author ehborisov
 */
@SuppressWarnings("deprecation")
public class AllureJunitPlatform implements TestExecutionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureJunitPlatform.class);

    private static final String STDOUT = "stdout";
    private static final String STDERR = "stderr";
    private static final String TEXT_PLAIN = "text/plain";
    private static final String TXT_EXTENSION = ".txt";

    private final ThreadLocal<TestPlan> testPlanStorage = new InheritableThreadLocal<>();

    private final Map<TestIdentifier, String> testUuids = new ConcurrentHashMap<>();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final AllureLifecycle lifecycle;

    public AllureJunitPlatform(final AllureLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    public AllureJunitPlatform() {
        this.lifecycle = Allure.getLifecycle();
    }

    public AllureLifecycle getLifecycle() {
        return lifecycle;
    }

    @Override
    public void testPlanExecutionStarted(final TestPlan testPlan) {
        testPlanStorage.set(testPlan);
    }

    @Override
    public void testPlanExecutionFinished(final TestPlan testPlan) {
        testPlanStorage.remove();
    }

    @Override
    public void executionStarted(final TestIdentifier testIdentifier) {
        if (testIdentifier.isTest()) {
            startTestCase(testIdentifier);
        }
    }

    @Override
    public void executionSkipped(final TestIdentifier testIdentifier, final String reason) {
        if (testIdentifier.isTest()) {
            startTestCase(testIdentifier);
            stopTestCase(testIdentifier, SKIPPED, new StatusDetails().setMessage(reason));
            return;
        }
        final TestPlan testPlan = testPlanStorage.get();
        if (Objects.nonNull(testPlan)) {
            final Set<TestIdentifier> children = testPlan.getChildren(testIdentifier);
            if (children.isEmpty()) {
                // this is probably test template, so we report it as-is
                startTestCase(testIdentifier);
                stopTestCase(testIdentifier, SKIPPED, new StatusDetails().setMessage(reason));
                return;
            }

            final Set<TestIdentifier> visited = new HashSet<>(Collections.singleton(testIdentifier));
            children.forEach(child -> executionSkipped(testPlan, child, reason, visited));
        }
    }

    private void executionSkipped(final TestPlan testPlan,
                                  final TestIdentifier testIdentifier,
                                  final String reason,
                                  final Set<TestIdentifier> visited) {
        if (testIdentifier.isTest()) {
            startTestCase(testIdentifier);
            stopTestCase(testIdentifier, SKIPPED, new StatusDetails().setMessage(reason));
            return;
        }
        final Set<TestIdentifier> children = testPlan.getChildren(testIdentifier);
        children.stream()
                .filter(visited::add)
                .forEach(child -> executionSkipped(testPlan, child, reason, visited));
    }

    @Override
    public void executionFinished(final TestIdentifier testIdentifier, final TestExecutionResult testExecutionResult) {
        if (testIdentifier.isTest()) {
            final Status status = extractStatus(testExecutionResult);
            final StatusDetails statusDetails = testExecutionResult.getThrowable()
                    .flatMap(ResultsUtils::getStatusDetails)
                    .orElse(null);
            stopTestCase(testIdentifier, status, statusDetails);
        }
    }

    @Override
    public void reportingEntryPublished(final TestIdentifier testIdentifier, final ReportEntry entry) {
        final String uuid = getUuid(testIdentifier);
        if (Objects.isNull(uuid)) {
            // no known test running at the moment
            return;
        }

        final Map<String, String> keyValuePairs = entry.getKeyValuePairs();
        if (keyValuePairs.containsKey(STDOUT)) {
            final String content = keyValuePairs.getOrDefault(STDOUT, "");
            getLifecycle().addAttachment("Stdout", TEXT_PLAIN, TXT_EXTENSION, content.getBytes(UTF_8));
        }
        if (keyValuePairs.containsKey(STDERR)) {
            final String content = keyValuePairs.getOrDefault(STDERR, "");
            getLifecycle().addAttachment("Stderr", TEXT_PLAIN, TXT_EXTENSION, content.getBytes(UTF_8));
        }

    }

    protected Status getStatus(final Throwable throwable) {
        return ResultsUtils.getStatus(throwable).orElse(FAILED);
    }

    @SuppressWarnings("PMD.NcssCount")
    private void startTestCase(final TestIdentifier testIdentifier) {
        final String uuid = createUuid(testIdentifier);

        final Optional<MethodSource> methodSource = testIdentifier.getSource()
                .filter(MethodSource.class::isInstance)
                .map(MethodSource.class::cast);

        final Optional<Method> testMethod = methodSource.flatMap(this::getTestMethod);
        final Optional<Class<?>> testClass = methodSource.flatMap(this::getTestClass);

        final TestResult result = new TestResult()
                .setUuid(uuid)
                .setName(testIdentifier.getDisplayName())
                .setLabels(getTags(testIdentifier))
                .setHistoryId(getHistoryId(testIdentifier))
                .setStage(Stage.RUNNING);

        result.getLabels().addAll(getProvidedLabels());

        testClass.map(this::getLabels).ifPresent(result.getLabels()::addAll);
        testMethod.map(this::getLabels).ifPresent(result.getLabels()::addAll);

        testClass.map(this::getLinks).ifPresent(result.getLinks()::addAll);
        testMethod.map(this::getLinks).ifPresent(result.getLinks()::addAll);

        result.getLabels().addAll(Arrays.asList(
                createHostLabel(),
                createThreadLabel(),
                createFrameworkLabel("junit-platform"),
                createLanguageLabel("java")
        ));

        methodSource.ifPresent(source -> {
            result.setFullName(String.format("%s.%s", source.getClassName(), source.getMethodName()));
            result.getLabels().addAll(Arrays.asList(
                    createPackageLabel(source.getClassName()),
                    createTestClassLabel(source.getClassName()),
                    createTestMethodLabel(source.getMethodName())
            ));
        });

        testClass.ifPresent(aClass -> {
            final String suiteName = getDisplayName(aClass).orElse(aClass.getCanonicalName());
            result.getLabels().add(createSuiteLabel(suiteName));
        });

        final Optional<String> classDescription = testClass.flatMap(this::getDescription);
        final Optional<String> methodDescription = testMethod.flatMap(this::getDescription);

        final String description = Stream.of(classDescription, methodDescription)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.joining("\n\n"));

        result.setDescription(description);

        testMethod.map(this::getSeverity)
                .filter(Optional::isPresent)
                .orElse(testClass.flatMap(this::getSeverity))
                .map(ResultsUtils::createSeverityLabel)
                .ifPresent(result.getLabels()::add);

        testMethod.map(this::getOwner)
                .filter(Optional::isPresent)
                .orElse(testClass.flatMap(this::getOwner))
                .map(ResultsUtils::createOwnerLabel)
                .ifPresent(result.getLabels()::add);

        testMethod.ifPresent(method -> ResultsUtils.processDescription(
                method.getDeclaringClass().getClassLoader(),
                method,
                result
        ));

        getLifecycle().scheduleTestCase(result);
        getLifecycle().startTestCase(uuid);
    }

    private void stopTestCase(final TestIdentifier testIdentifier,
                              final Status status,
                              final StatusDetails statusDetails) {

        final String uuid = removeUuid(testIdentifier);
        getLifecycle().updateTestCase(uuid, result -> {
            result.setStage(Stage.FINISHED);
            result.setStatus(status);
            result.setStatusDetails(statusDetails);
        });
        getLifecycle().stopTestCase(uuid);
        getLifecycle().writeTestCase(uuid);
    }

    private String createUuid(final TestIdentifier testIdentifier) {
        final String uuid = UUID.randomUUID().toString();
        try {
            lock.writeLock().lock();
            testUuids.put(testIdentifier, uuid);
        } finally {
            lock.writeLock().unlock();
        }
        return uuid;
    }

    private String getUuid(final TestIdentifier testIdentifier) {
        try {
            lock.readLock().lock();
            return testUuids.get(testIdentifier);
        } finally {
            lock.readLock().unlock();
        }
    }

    private String removeUuid(final TestIdentifier testIdentifier) {
        try {
            lock.writeLock().lock();
            return testUuids.remove(testIdentifier);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private Status extractStatus(final TestExecutionResult testExecutionResult) {
        switch (testExecutionResult.getStatus()) {
            case FAILED:
                return testExecutionResult.getThrowable().isPresent()
                        ? getStatus(testExecutionResult.getThrowable().get())
                        : FAILED;
            case SUCCESSFUL:
                return PASSED;
            default:
                return SKIPPED;
        }
    }

    private List<Label> getTags(final TestIdentifier testIdentifier) {
        return testIdentifier.getTags().stream()
                .map(TestTag::getName)
                .map(ResultsUtils::createTagLabel)
                .collect(Collectors.toList());
    }

    protected String getHistoryId(final TestIdentifier testIdentifier) {
        return md5(testIdentifier.getUniqueId());
    }

    private String md5(final String source) {
        final byte[] bytes = getMd5Digest().digest(source.getBytes(UTF_8));
        return new BigInteger(1, bytes).toString(16);
    }

    private Optional<SeverityLevel> getSeverity(final AnnotatedElement annotatedElement) {
        return getAnnotations(annotatedElement, Severity.class)
                .map(Severity::value)
                .findAny();
    }

    private Optional<String> getOwner(final AnnotatedElement annotatedElement) {
        return getAnnotations(annotatedElement, Owner.class)
                .map(Owner::value)
                .findAny();
    }

    private Optional<String> getDisplayName(final AnnotatedElement annotatedElement) {
        return getAnnotations(annotatedElement, DisplayName.class)
                .map(DisplayName::value)
                .findAny();
    }

    private Optional<String> getDescription(final AnnotatedElement annotatedElement) {
        return getAnnotations(annotatedElement, Description.class)
                .map(Description::value)
                .findAny();
    }

    private List<Label> getLabels(final AnnotatedElement annotatedElement) {
        return Stream.of(
                getAnnotations(annotatedElement, Epic.class).map(ResultsUtils::createLabel),
                getAnnotations(annotatedElement, Feature.class).map(ResultsUtils::createLabel),
                getAnnotations(annotatedElement, Story.class).map(ResultsUtils::createLabel)
        ).reduce(Stream::concat).orElseGet(Stream::empty).collect(Collectors.toList());
    }

    private List<Link> getLinks(final AnnotatedElement annotatedElement) {
        return Stream.of(
                getAnnotations(annotatedElement, io.qameta.allure.Link.class).map(ResultsUtils::createLink),
                getAnnotations(annotatedElement, io.qameta.allure.Issue.class).map(ResultsUtils::createLink),
                getAnnotations(annotatedElement, io.qameta.allure.TmsLink.class).map(ResultsUtils::createLink))
                .reduce(Stream::concat).orElseGet(Stream::empty).collect(Collectors.toList());
    }


    private <T extends Annotation> Stream<T> getAnnotations(final AnnotatedElement annotatedElement,
                                                            final Class<T> annotationClass) {
        final T annotation = annotatedElement.getAnnotation(annotationClass);
        return Stream.concat(
                extractRepeatable(annotatedElement, annotationClass).stream(),
                Objects.isNull(annotation) ? Stream.empty() : Stream.of(annotation)
        );
    }

    @SuppressWarnings("unchecked")
    private <T extends Annotation> List<T> extractRepeatable(final AnnotatedElement annotatedElement,
                                                             final Class<T> annotationClass) {
        if (annotationClass.isAnnotationPresent(Repeatable.class)) {
            final Repeatable repeatable = annotationClass.getAnnotation(Repeatable.class);
            final Class<? extends Annotation> wrapper = repeatable.value();
            final Annotation annotation = annotatedElement.getAnnotation(wrapper);
            if (Objects.nonNull(annotation)) {
                try {
                    final Method value = annotation.getClass().getMethod("value");
                    final Object annotations = value.invoke(annotation);
                    return Arrays.asList((T[]) annotations);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        return Collections.emptyList();
    }

    private Optional<Class<?>> getTestClass(final MethodSource source) {
        try {
            return Optional.of(Class.forName(source.getClassName()));
        } catch (ClassNotFoundException e) {
            LOGGER.trace("Could not get test class from method source {}", source, e);
        }
        return Optional.empty();
    }

    private Optional<Method> getTestMethod(final MethodSource source) {
        try {
            final Class<?> aClass = Class.forName(source.getClassName());
            return Stream.of(aClass.getDeclaredMethods())
                    .filter(method -> MethodSource.from(method).equals(source))
                    .findAny();
        } catch (ClassNotFoundException e) {
            LOGGER.trace("Could not get test method from method source {}", source, e);
        }
        return Optional.empty();
    }
}
