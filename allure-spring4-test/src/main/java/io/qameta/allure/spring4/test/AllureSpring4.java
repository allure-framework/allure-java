package io.qameta.allure.spring4.test;

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
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.qameta.allure.util.ResultsUtils.bytesToHex;
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
import static io.qameta.allure.util.ResultsUtils.getStatus;
import static io.qameta.allure.util.ResultsUtils.getStatusDetails;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author charlie (Dmitry Baev).
 */
@SuppressWarnings("PMD.ExcessiveImports")
public class AllureSpring4 implements TestExecutionListener {

    private final ThreadLocal<String> testCases = new InheritableThreadLocal<String>() {
        @Override
        protected String initialValue() {
            return UUID.randomUUID().toString();
        }
    };

    private final AllureLifecycle lifecycle;

    public AllureSpring4() {
        this.lifecycle = Allure.getLifecycle();
    }

    @Override
    public void beforeTestClass(final TestContext testContext) {
        //do nothing
    }

    @Override
    public void prepareTestInstance(final TestContext testContext) {
        //do nothing
    }

    @Override
    public void beforeTestMethod(final TestContext testContext) {
        final String uuid = testCases.get();
        final Class<?> testClass = testContext.getTestClass();
        final Method testMethod = testContext.getTestMethod();
        final String id = getHistoryId(testClass, testMethod);

        final String fullName = String.format("%s.%s", testClass.getCanonicalName(), testMethod.getName());
        final TestResult testResult = new TestResult()
                .setUuid(uuid)
                .setHistoryId(id)
                .setFullName(fullName)
                .setName(testMethod.getName());

        testResult.getLabels().addAll(getProvidedLabels());
        testResult.getLabels().addAll(Arrays.asList(
                createPackageLabel(testClass.getCanonicalName()),
                createTestClassLabel(testClass.getCanonicalName()),
                createTestMethodLabel(testMethod.getName()),
                createSuiteLabel(testClass.getName()),
                createHostLabel(),
                createThreadLabel(),
                createFrameworkLabel("spring4-test"),
                createLanguageLabel("java")
        ));
        testResult.getLabels().addAll(getLabels(testClass, testMethod));

        testResult.getLinks().addAll(getLinks(testClass, testMethod));

        getDisplayName(testMethod).ifPresent(testResult::setName);
        getLifecycle().scheduleTestCase(testResult);
        getLifecycle().startTestCase(uuid);
    }

    @Override
    public void afterTestMethod(final TestContext testContext) {
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
    public void afterTestClass(final TestContext testContext) {
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
        final MessageDigest digest = getMd5Digest();
        digest.update(testClass.getCanonicalName().getBytes(UTF_8));
        digest.update(testMethod.getName().getBytes(UTF_8));
        Stream.of(testMethod.getParameterTypes())
                .map(Class::getCanonicalName)
                .map(name -> name.getBytes(UTF_8))
                .forEach(digest::update);
        return bytesToHex(digest.digest());
    }
}
