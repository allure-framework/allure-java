package io.qameta.allure.junit5;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.Description;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.util.ResultsUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.qameta.allure.model.Status.FAILED;
import static io.qameta.allure.model.Status.PASSED;
import static io.qameta.allure.model.Status.SKIPPED;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author ehborisov
 */
public class AllureJunit5 implements TestExecutionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureJunit5.class);

    private static final String TAG = "tag";
    private static final String SUITE = "suite";
    private static final String PACKAGE = "package";


    private final ThreadLocal<String> tests
            = InheritableThreadLocal.withInitial(() -> UUID.randomUUID().toString());

    private final AllureLifecycle lifecycle;

    public AllureJunit5(final AllureLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    public AllureJunit5() {
        this.lifecycle = Allure.getLifecycle();
    }

    public AllureLifecycle getLifecycle() {
        return lifecycle;
    }

    @Override
    public void executionStarted(final TestIdentifier testIdentifier) {
        if (testIdentifier.isTest()) {
            final Optional<MethodSource> methodSource = testIdentifier.getSource()
                    .filter(MethodSource.class::isInstance)
                    .map(MethodSource.class::cast);
            final String uuid = tests.get();
            final TestResult result = new TestResult()
                    .withUuid(uuid)
                    .withName(testIdentifier.getDisplayName())
                    .withLabels(getTags(testIdentifier))
                    .withHistoryId(getHistoryId(testIdentifier))
                    .withStage(Stage.RUNNING);

            methodSource.ifPresent(source -> {
                result.setDescription(getDescription(source));
                result.getLabels().add(new Label().setName(SUITE).setValue(getSuite(source)));
                result.getLabels().add(new Label().setName(PACKAGE).setValue(source.getClassName()));
            });

            getLifecycle().scheduleTestCase(result);
            getLifecycle().startTestCase(uuid);
        }
    }

    @Override
    public void executionSkipped(final TestIdentifier testIdentifier, final String reason) {
        if (testIdentifier.isTest()) {
            final String uuid = tests.get();
            final TestResult result = new TestResult()
                    .withUuid(uuid)
                    .withName(testIdentifier.getDisplayName())
                    .withLabels(getTags(testIdentifier))
                    .withHistoryId(getHistoryId(testIdentifier))
                    .withStage(Stage.RUNNING);

            testIdentifier
                    .getSource()
                    .filter(MethodSource.class::isInstance)
                    .map(MethodSource.class::cast)
                    .ifPresent(source -> {
                        result.setDescription(getDescription(source));
                        result.getLabels().add(new Label().setName(SUITE).setValue(getSuite(source)));
                        result.getLabels().add(new Label().setName(PACKAGE).setValue(source.getClassName()));
                    });

            getLifecycle().scheduleTestCase(result);
            getLifecycle().startTestCase(uuid);

            tests.remove();
            getLifecycle().updateTestCase(uuid, testResult -> {
                testResult.setStage(Stage.FINISHED);
                testResult.setStatus(SKIPPED);
            });
            getLifecycle().stopTestCase(uuid);
            getLifecycle().writeTestCase(uuid);
        }
    }

    @Override
    public void executionFinished(final TestIdentifier testIdentifier, final TestExecutionResult testExecutionResult) {
        if (testIdentifier.isTest()) {
            final String uuid = tests.get();
            tests.remove();
            getLifecycle().updateTestCase(uuid, result -> {
                result.setStage(Stage.FINISHED);
                switch (testExecutionResult.getStatus()) {
                    case FAILED:
                        testExecutionResult.getThrowable().ifPresent(throwable -> {
                            result.setStatus(getStatus(throwable));
                            result.setStatusDetails(ResultsUtils.getStatusDetails(throwable).orElse(null));
                        });
                        break;
                    case SUCCESSFUL:
                        result.setStatus(PASSED);
                        break;
                    default:
                        result.setStatus(SKIPPED);
                        testExecutionResult.getThrowable().ifPresent(throwable ->
                                result.setStatusDetails(ResultsUtils.getStatusDetails(throwable).orElse(null))
                        );
                        break;
                }
            });
            getLifecycle().stopTestCase(uuid);
            getLifecycle().writeTestCase(uuid);
        }
    }

    protected Status getStatus(final Throwable throwable) {
        return ResultsUtils.getStatus(throwable).orElse(FAILED);
    }

    private List<Label> getTags(final TestIdentifier testIdentifier) {
        return testIdentifier.getTags().stream()
                .map(tag -> new Label().setName(TAG).setValue(tag.getName()))
                .collect(Collectors.toList());
    }

    protected String getHistoryId(final TestIdentifier testIdentifier) {
        return md5(testIdentifier.getUniqueId());
    }

    private String md5(final String source) {
        final byte[] bytes = getMessageDigest().digest(source.getBytes(UTF_8));
        return new BigInteger(1, bytes).toString(16);
    }

    private MessageDigest getMessageDigest() {
        try {
            return MessageDigest.getInstance("md5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Could not find md5 hashing algorithm", e);
        }
    }

    private String getSuite(final MethodSource source) {
        try {
            final DisplayName displayNameAnnotation =
                    Class.forName(source.getClassName()).getAnnotation(DisplayName.class);
            if (displayNameAnnotation != null && !displayNameAnnotation.value().isEmpty()) {
                return displayNameAnnotation.value();
            }
        } catch (ClassNotFoundException e) {
            LOGGER.trace(e.getMessage(), e);
        }
        return source.getClassName();
    }

    private String getDescription(final MethodSource source) {
        try {
            final Description descriptionAnnotation = Class.forName(source.getClassName())
                    .getDeclaredMethod(source.getMethodName())
                    .getAnnotation(Description.class);
            if (descriptionAnnotation != null) {
                return descriptionAnnotation.value();
            }
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            LOGGER.trace(e.getMessage(), e);
        }
        return null;
    }
}
