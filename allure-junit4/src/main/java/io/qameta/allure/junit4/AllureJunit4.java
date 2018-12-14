package io.qameta.allure.junit4;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.Story;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.util.ResultsUtils;
import org.junit.Ignore;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.qameta.allure.util.ResultsUtils.createHostLabel;
import static io.qameta.allure.util.ResultsUtils.createPackageLabel;
import static io.qameta.allure.util.ResultsUtils.createSuiteLabel;
import static io.qameta.allure.util.ResultsUtils.createTestClassLabel;
import static io.qameta.allure.util.ResultsUtils.createTestMethodLabel;
import static io.qameta.allure.util.ResultsUtils.createThreadLabel;
import static io.qameta.allure.util.ResultsUtils.getProvidedLabels;
import static io.qameta.allure.util.ResultsUtils.getStatus;
import static io.qameta.allure.util.ResultsUtils.getStatusDetails;
import static io.qameta.allure.util.ResultsUtils.md5;

/**
 * Allure Junit4 listener.
 */
@RunListener.ThreadSafe
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.CouplingBetweenObjects", "checkstyle:ClassFanOutComplexity"})
public class AllureJunit4 extends RunListener {

    private final ThreadLocal<String> testCases = new InheritableThreadLocal<String>() {
        @Override
        protected String initialValue() {
            return UUID.randomUUID().toString();
        }
    };

    private final AllureLifecycle lifecycle;

    public AllureJunit4() {
        this(Allure.getLifecycle());
    }

    public AllureJunit4(final AllureLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    public AllureLifecycle getLifecycle() {
        return lifecycle;
    }

    @Override
    public void testRunStarted(final Description description) {
        //do nothing
    }

    @Override
    public void testRunFinished(final Result result) {
        //do nothing
    }

    @Override
    public void testStarted(final Description description) {
        final String uuid = testCases.get();
        final TestResult result = createTestResult(uuid, description);
        getLifecycle().scheduleTestCase(result);
        getLifecycle().startTestCase(uuid);
    }

    @Override
    public void testFinished(final Description description) {
        final String uuid = testCases.get();
        testCases.remove();
        getLifecycle().updateTestCase(uuid, testResult -> {
            if (Objects.isNull(testResult.getStatus())) {
                testResult.setStatus(Status.PASSED);
            }
        });

        getLifecycle().stopTestCase(uuid);
        getLifecycle().writeTestCase(uuid);
    }

    @Override
    public void testFailure(final Failure failure) {
        final String uuid = testCases.get();
        getLifecycle().updateTestCase(uuid, testResult -> testResult
                .setStatus(getStatus(failure.getException()).orElse(null))
                .setStatusDetails(getStatusDetails(failure.getException()).orElse(null))
        );
    }

    @Override
    public void testAssumptionFailure(final Failure failure) {
        final String uuid = testCases.get();
        getLifecycle().updateTestCase(uuid, testResult ->
                testResult.setStatus(Status.SKIPPED)
                        .setStatusDetails(getStatusDetails(failure.getException()).orElse(null))
        );
    }

    @Override
    public void testIgnored(final Description description) {
        final String uuid = testCases.get();
        testCases.remove();

        final TestResult result = createTestResult(uuid, description);
        result.setStatus(Status.SKIPPED);
        result.setStatusDetails(getIgnoredMessage(description));
        result.setStart(System.currentTimeMillis());

        getLifecycle().scheduleTestCase(result);
        getLifecycle().stopTestCase(uuid);
        getLifecycle().writeTestCase(uuid);
    }

    private Optional<String> getDisplayName(final Description result) {
        return Optional.ofNullable(result.getAnnotation(DisplayName.class))
                .map(DisplayName::value);
    }

    private Optional<String> getDescription(final Description result) {
        return Optional.ofNullable(result.getAnnotation(io.qameta.allure.Description.class))
                .map(io.qameta.allure.Description::value);
    }

    private List<Link> getLinks(final Description result) {
        return Stream.of(
                getAnnotationsOnClass(result, io.qameta.allure.Link.class).stream().map(ResultsUtils::createLink),
                getAnnotationsOnMethod(result, io.qameta.allure.Link.class).stream().map(ResultsUtils::createLink),
                getAnnotationsOnClass(result, io.qameta.allure.Issue.class).stream().map(ResultsUtils::createLink),
                getAnnotationsOnMethod(result, io.qameta.allure.Issue.class).stream().map(ResultsUtils::createLink),
                getAnnotationsOnClass(result, io.qameta.allure.TmsLink.class).stream().map(ResultsUtils::createLink),
                getAnnotationsOnMethod(result, io.qameta.allure.TmsLink.class).stream().map(ResultsUtils::createLink)
        ).reduce(Stream::concat).orElseGet(Stream::empty).collect(Collectors.toList());
    }

    private List<Label> getLabels(final Description result) {
        return Stream.of(
                getLabels(result, Epic.class, ResultsUtils::createLabel),
                getLabels(result, Feature.class, ResultsUtils::createLabel),
                getLabels(result, Story.class, ResultsUtils::createLabel),
                getLabels(result, Severity.class, ResultsUtils::createLabel),
                getLabels(result, Owner.class, ResultsUtils::createLabel),
                getLabels(result, Tag.class, this::createLabel)
        ).reduce(Stream::concat).orElseGet(Stream::empty).collect(Collectors.toList());
    }

    private <T extends Annotation> Stream<Label> getLabels(final Description result, final Class<T> labelAnnotation,
                                                           final Function<T, Label> extractor) {

        final List<Label> labels = getAnnotationsOnMethod(result, labelAnnotation).stream()
                .map(extractor)
                .collect(Collectors.toList());

        if (labelAnnotation.isAnnotationPresent(Repeatable.class) || labels.isEmpty()) {
            final Stream<Label> onClassLabels = getAnnotationsOnClass(result, labelAnnotation).stream()
                    .map(extractor);
            labels.addAll(onClassLabels.collect(Collectors.toList()));
        }

        return labels.stream();
    }

    private Label createLabel(final Tag tag) {
        return new Label().setName("tag").setValue(tag.value());
    }

    private <T extends Annotation> List<T> getAnnotationsOnMethod(final Description result, final Class<T> clazz) {
        final T annotation = result.getAnnotation(clazz);
        return Stream.concat(
                extractRepeatable(result, clazz).stream(),
                Objects.isNull(annotation) ? Stream.empty() : Stream.of(annotation)
        ).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private <T extends Annotation> List<T> extractRepeatable(final Description result, final Class<T> clazz) {
        if (clazz != null && clazz.isAnnotationPresent(Repeatable.class)) {
            final Repeatable repeatable = clazz.getAnnotation(Repeatable.class);
            final Class<? extends Annotation> wrapper = repeatable.value();
            final Annotation annotation = result.getAnnotation(wrapper);
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

    private <T extends Annotation> List<T> getAnnotationsOnClass(final Description result, final Class<T> clazz) {
        return Stream.of(result)
                .map(Description::getTestClass)
                .filter(Objects::nonNull)
                .map(testClass -> testClass.getAnnotationsByType(clazz))
                .flatMap(Stream::of)
                .collect(Collectors.toList());
    }

    private String getHistoryId(final Description description) {
        return md5(description.getClassName() + description.getMethodName());
    }

    private String getPackage(final Class<?> testClass) {
        return Optional.ofNullable(testClass)
                .map(Class::getPackage)
                .map(Package::getName)
                .orElse("");
    }

    private StatusDetails getIgnoredMessage(final Description description) {
        final Ignore ignore = description.getAnnotation(Ignore.class);
        final String message = Objects.nonNull(ignore) && !ignore.value().isEmpty()
                ? ignore.value() : "Test ignored (without reason)!";
        return new StatusDetails().setMessage(message);
    }

    private TestResult createTestResult(final String uuid, final Description description) {
        final String className = description.getClassName();
        final String methodName = description.getMethodName();
        final String name = Objects.nonNull(methodName) ? methodName : className;
        final String fullName = Objects.nonNull(methodName) ? String.format("%s.%s", className, methodName) : className;
        final String suite = Optional.ofNullable(description.getTestClass())
                .map(it -> it.getAnnotation(DisplayName.class))
                .map(DisplayName::value).orElse(className);

        final TestResult testResult = new TestResult()
                .setUuid(uuid)
                .setHistoryId(getHistoryId(description))
                .setFullName(fullName)
                .setName(name);

        testResult.getLabels().addAll(getProvidedLabels());
        testResult.getLabels().addAll(Arrays.asList(
                createPackageLabel(getPackage(description.getTestClass())),
                createTestClassLabel(className),
                createTestMethodLabel(name),
                createSuiteLabel(suite),
                createHostLabel(),
                createThreadLabel()
        ));
        testResult.getLabels().addAll(getLabels(description));
        testResult.getLinks().addAll(getLinks(description));

        getDisplayName(description).ifPresent(testResult::setName);
        getDescription(description).ifPresent(testResult::setDescription);
        return testResult;
    }

}
