package io.qameta.allure.spring4;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.Story;
import io.qameta.allure.TmsLink;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.util.ResultsUtils;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.qameta.allure.util.ResultsUtils.getHostName;
import static io.qameta.allure.util.ResultsUtils.getStatus;
import static io.qameta.allure.util.ResultsUtils.getStatusDetails;
import static io.qameta.allure.util.ResultsUtils.getThreadName;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author charlie (Dmitry Baev).
 */
@SuppressWarnings("PMD.ExcessiveImports")
public class AllureSpring4 implements TestExecutionListener {

    private static final String MD_5 = "md5";

    private final ThreadLocal<String> testCases
            = InheritableThreadLocal.withInitial(() -> UUID.randomUUID().toString());

    private final AllureLifecycle lifecycle;

    public AllureSpring4() {
        this.lifecycle = Allure.getLifecycle();
    }

    @Override
    public void beforeTestClass(final TestContext testContext) throws Exception {
        //do nothing
    }

    @Override
    public void prepareTestInstance(final TestContext testContext) throws Exception {
        //do nothing
    }

    @Override
    public void beforeTestMethod(final TestContext testContext) throws Exception {
        final String uuid = testCases.get();
        final Class<?> testClass = testContext.getTestClass();
        final Method testMethod = testContext.getTestMethod();
        final String id = getHistoryId(testClass, testMethod);

        final TestResult result = new TestResult()
                .setUuid(uuid)
                .setHistoryId(id)
                .setName(testMethod.getName())
                .setFullName(String.format("%s.%s", testClass.getCanonicalName(), testMethod.getName()))
                .setLinks(getLinks(testClass, testMethod))
                .setLabels(
                        new Label().setName("package").setValue(testClass.getCanonicalName()),
                        new Label().setName("testClass").setValue(testClass.getCanonicalName()),
                        new Label().setName("testMethod").setValue(testMethod.getName()),

                        new Label().setName("suite").setValue(testClass.getName()),

                        new Label().setName("host").setValue(getHostName()),
                        new Label().setName("thread").setValue(getThreadName())
                );

        result.getLabels().addAll(getLabels(testClass, testMethod));
        getDisplayName(testMethod).ifPresent(result::setName);
        getLifecycle().scheduleTestCase(result);
        getLifecycle().startTestCase(uuid);
    }

    @Override
    public void afterTestMethod(final TestContext testContext) throws Exception {
        final String uuid = testCases.get();
        testCases.remove();
        getLifecycle().updateTestCase(uuid, testResult -> {
            testResult.setStatus(getStatus(testContext.getTestException()).orElse(Status.PASSED));
            if (Objects.isNull(testResult.getStatusDetails())) {
                testResult.setStatusDetails(new StatusDetails());
            }
            getStatusDetails(testContext.getTestException()).ifPresent(statusDetails -> {
                testResult.getStatusDetails().setMessage(statusDetails.getMessage());
                testResult.getStatusDetails().setTrace(statusDetails.getTrace());
            });
        });
        getLifecycle().stopTestCase(uuid);
        getLifecycle().writeTestCase(uuid);
    }

    @Override
    public void afterTestClass(final TestContext testContext) throws Exception {
        //do nothing
    }

    public AllureLifecycle getLifecycle() {
        return lifecycle;
    }

    private Optional<String> getDisplayName(final Method method) {
        return Optional.ofNullable(method.getAnnotation(DisplayName.class))
                .map(DisplayName::value);
    }

    private List<Link> getLinks(final Class<?> testClass, final Method testMethod) {
        return Stream.of(
                getAnnotations(testClass, testMethod, io.qameta.allure.Link.class).map(ResultsUtils::createLink),
                getAnnotations(testClass, testMethod, Issue.class).map(ResultsUtils::createLink),
                getAnnotations(testClass, testMethod, TmsLink.class).map(ResultsUtils::createLink)
        ).reduce(Stream::concat).orElseGet(Stream::empty).collect(Collectors.toList());
    }

    private List<Label> getLabels(final Class<?> testClass, final Method testMethod) {
        return Stream.of(
                getAnnotations(testClass, testMethod, Epic.class).map(ResultsUtils::createLabel),
                getAnnotations(testClass, testMethod, Feature.class).map(ResultsUtils::createLabel),
                getAnnotations(testClass, testMethod, Story.class).map(ResultsUtils::createLabel),
                getAnnotations(testClass, testMethod, Severity.class).map(ResultsUtils::createLabel),
                getAnnotations(testClass, testMethod, Owner.class).map(ResultsUtils::createLabel)
        ).reduce(Stream::concat).orElseGet(Stream::empty).collect(Collectors.toList());
    }

    private <T extends Annotation> Stream<T> getAnnotations(
            final Class<?> testClass, final Method testMethod, final Class<T> annotation) {
        return Stream.of(annotation)
                .flatMap(clazz -> Stream.concat(
                        Stream.of(testClass.getAnnotationsByType(clazz)),
                        Stream.of(testMethod.getAnnotationsByType(clazz)))
                );
    }

    private String getHistoryId(final Class<?> testClass, final Method testMethod) {
        final MessageDigest digest = getMessageDigest();
        digest.update(testClass.getCanonicalName().getBytes(UTF_8));
        digest.update(testMethod.getName().getBytes(UTF_8));
        Stream.of(testMethod.getParameterTypes())
                .map(Class::getCanonicalName)
                .map(name -> name.getBytes(UTF_8))
                .forEach(digest::update);
        return new BigInteger(1, digest.digest()).toString(16);
    }

    private MessageDigest getMessageDigest() {
        try {
            return MessageDigest.getInstance(MD_5);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Could not find md5 hashing algorithm", e);
        }
    }
}
