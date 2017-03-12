package io.qameta.allure.junit4;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.ResultsUtils;
import io.qameta.allure.Severity;
import io.qameta.allure.Story;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.TestResult;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.qameta.allure.ResultsUtils.getHostName;
import static io.qameta.allure.ResultsUtils.getStatus;
import static io.qameta.allure.ResultsUtils.getStatusDetails;
import static io.qameta.allure.ResultsUtils.getThreadName;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author charlie (Dmitry Baev).
 */
@RunListener.ThreadSafe
public class AllureJunit4 extends RunListener {

    public static final String MD_5 = "md5";

    private final ThreadLocal<String> testCases
            = InheritableThreadLocal.withInitial(() -> UUID.randomUUID().toString());

    private final AllureLifecycle lifecycle;

    public AllureJunit4() {
        this(Allure.getLifecycle());
    }

    public AllureJunit4(AllureLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    public AllureLifecycle getLifecycle() {
        return lifecycle;
    }

    @Override
    public void testRunStarted(Description description) throws Exception {
    }

    @Override
    public void testRunFinished(Result result) throws Exception {
    }

    @Override
    public void testStarted(Description description) throws Exception {
        String uuid = testCases.get();
        String id = getHistoryId(description);

        TestResult result = new TestResult()
                .withUuid(uuid)
                .withHistoryId(id)
                .withName(description.getMethodName())
                .withFullName(String.format("%s.%s", description.getClassName(), description.getMethodName()))
                .withLinks(getLinks(description))
                .withLabels(
                        new Label().withName("package").withValue(getPackage(description.getTestClass())),
                        new Label().withName("testClass").withValue(description.getClassName()),
                        new Label().withName("testMethod").withValue(description.getMethodName()),

                        new Label().withName("suite").withValue(description.getClassName()),

                        new Label().withName("host").withValue(getHostName()),
                        new Label().withName("thread").withValue(getThreadName())
                );

        result.getLabels().addAll(getLabels(description));
        getDisplayName(description).ifPresent(result::setName);
        getLifecycle().scheduleTestCase(result);
        getLifecycle().startTestCase(uuid);
    }

    @Override
    public void testFinished(Description description) throws Exception {
        String uuid = testCases.get();
        getLifecycle().updateTestCase(uuid, testResult -> {
            if (Objects.isNull(testResult.getStatus())) {
                testResult.setStatus(Status.PASSED);
            }
        });

        getLifecycle().stopTestCase(uuid);
        getLifecycle().writeTestCase(uuid);
    }

    @Override
    public void testFailure(Failure failure) throws Exception {
        String uuid = testCases.get();
        getLifecycle().updateTestCase(uuid, testResult -> testResult
                .withStatus(getStatus(failure.getException()).orElse(null))
                .withStatusDetails(getStatusDetails(failure.getException()).orElse(null))
        );
    }

    @Override
    public void testAssumptionFailure(Failure failure) {
    }

    @Override
    public void testIgnored(Description description) throws Exception {
    }

    private Optional<String> getDisplayName(Description result) {
        return Optional.ofNullable(result.getAnnotation(DisplayName.class))
                .map(DisplayName::value);
    }

    private List<Link> getLinks(Description result) {
        return Stream.of(
                getAnnotationsOnClass(result, io.qameta.allure.Link.class).stream().map(ResultsUtils::createLink),
                getAnnotationsOnMethod(result, io.qameta.allure.Link.class).stream().map(ResultsUtils::createLink),
                getAnnotationsOnClass(result, io.qameta.allure.Issue.class).stream().map(ResultsUtils::createLink),
                getAnnotationsOnMethod(result, io.qameta.allure.Issue.class).stream().map(ResultsUtils::createLink),
                getAnnotationsOnClass(result, io.qameta.allure.TmsLink.class).stream().map(ResultsUtils::createLink),
                getAnnotationsOnMethod(result, io.qameta.allure.TmsLink.class).stream().map(ResultsUtils::createLink)
        ).reduce(Stream::concat).orElseGet(Stream::empty).collect(Collectors.toList());
    }

    private List<Label> getLabels(Description result) {
        return Stream.of(
                getLabels(result, Epic.class, ResultsUtils::createLabel),
                getLabels(result, Feature.class, ResultsUtils::createLabel),
                getLabels(result, Story.class, ResultsUtils::createLabel),
                getLabels(result, Severity.class, ResultsUtils::createLabel),
                getLabels(result, Owner.class, ResultsUtils::createLabel)
        ).reduce(Stream::concat).orElseGet(Stream::empty).collect(Collectors.toList());
    }

    private <T extends Annotation> Stream<Label> getLabels(Description result, Class<T> clazz,
                                                           Function<T, Label> extractor) {

        List<Label> onMethod = getAnnotationsOnMethod(result, clazz).stream()
                .map(extractor)
                .collect(Collectors.toList());
        if (!onMethod.isEmpty()) {
            return onMethod.stream();
        }
        return getAnnotationsOnClass(result, clazz).stream()
                .map(extractor);
    }

    private <T extends Annotation> List<T> getAnnotationsOnMethod(Description result, Class<T> clazz) {
        T annotation = result.getAnnotation(clazz);
        return Stream.concat(
                extractRepeatable(result, clazz).stream(),
                Objects.isNull(annotation) ? Stream.empty() : Stream.of(annotation)
        ).collect(Collectors.toList());
    }

    private <T extends Annotation> List<T> extractRepeatable(Description result, Class<T> clazz) {
        if (clazz.isAnnotationPresent(Repeatable.class)) {
            Repeatable repeatable = clazz.getAnnotation(Repeatable.class);
            Class<? extends Annotation> wrapper = repeatable.value();
            Annotation annotation = result.getAnnotation(wrapper);
            if (Objects.nonNull(annotation)) {
                try {
                    Method value = annotation.getClass().getMethod("value");
                    Object annotations = value.invoke(annotation);
                    //noinspection unchecked
                    return Arrays.asList((T[]) annotations);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        return Collections.emptyList();
    }

    private <T extends Annotation> List<T> getAnnotationsOnClass(Description result, Class<T> clazz) {
        return Stream.of(result)
                .map(Description::getTestClass)
                .map(testClass -> testClass.getAnnotationsByType(clazz))
                .flatMap(Stream::of)
                .collect(Collectors.toList());
    }

    private String getHistoryId(Description description) {
        return md5(description.getClassName() + description.getMethodName());
    }

    private String md5(String source) {
        byte[] bytes = getMessageDigest().digest(source.getBytes(UTF_8));
        return new BigInteger(1, bytes).toString(16);
    }

    private MessageDigest getMessageDigest() {
        try {
            return MessageDigest.getInstance(MD_5);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Could not find md5 hashing algorithm");
        }
    }

    private String getPackage(Class<?> testClass) {
        return testClass.getPackage().getName();
    }
}
