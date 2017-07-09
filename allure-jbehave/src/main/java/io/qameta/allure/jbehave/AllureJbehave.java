package io.qameta.allure.jbehave;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.util.ResultsUtils;
import org.jbehave.core.model.Story;
import org.jbehave.core.reporters.NullStoryReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static io.qameta.allure.util.ResultsUtils.createHostLabel;
import static io.qameta.allure.util.ResultsUtils.createStoryLabel;
import static io.qameta.allure.util.ResultsUtils.createThreadLabel;

/**
 * @author charlie (Dmitry Baev).
 */
public class AllureJbehave extends NullStoryReporter {

    private static final String MD_5 = "md5";

    private final AllureLifecycle lifecycle;

    private final ThreadLocal<Story> stories = new InheritableThreadLocal<>();

    private final ThreadLocal<String> scenarios
            = InheritableThreadLocal.withInitial(() -> UUID.randomUUID().toString());

    private final Map<String, Status> scenarioStatusStorage = new ConcurrentHashMap<>();

    public AllureJbehave() {
        this(Allure.getLifecycle());
    }

    public AllureJbehave(final AllureLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    @Override
    public void beforeStory(final Story story, final boolean givenStory) {
        stories.set(story);
    }

    @Override
    public void afterStory(final boolean givenStory) {
        stories.remove();
    }

    @Override
    public void beforeScenario(final String title) {
        final Story story = stories.get();
        final String uuid = scenarios.get();
        final String fullName = String.format("%s: %s", story.getName(), title);
        final TestResult result = new TestResult()
                .withUuid(uuid)
                .withName(title)
                .withFullName(fullName)
                .withStage(Stage.SCHEDULED)
                .withLabels(createStoryLabel(story.getName()), createHostLabel(), createThreadLabel())
                .withDescription(story.getDescription().asString())
                .withHistoryId(md5(fullName));
        getLifecycle().scheduleTestCase(result);
        getLifecycle().startTestCase(result.getUuid());
    }

    @Override
    public void afterScenario() {
        final String uuid = scenarios.get();
        final Status status = scenarioStatusStorage.getOrDefault(uuid, Status.PASSED);

        getLifecycle().updateTestCase(uuid, testResult -> testResult.withStatus(status));
        getLifecycle().stopTestCase(uuid);
        getLifecycle().writeTestCase(uuid);
        scenarios.remove();
    }

    @Override
    public void beforeStep(final String step) {
        final String stepUuid = UUID.randomUUID().toString();
        getLifecycle().startStep(stepUuid, new StepResult().withName(step));
    }

    @Override
    public void successful(final String step) {
        getLifecycle().updateStep(result -> result.withStatus(Status.PASSED));
        getLifecycle().stopStep();
        updateScenarioStatus(Status.PASSED);
    }

    @Override
    public void ignorable(final String step) {
        getLifecycle().updateStep(result -> result.withStatus(Status.SKIPPED));
        getLifecycle().stopStep();
        updateScenarioStatus(Status.SKIPPED);
    }

    @Override
    public void pending(final String step) {
        getLifecycle().updateStep(result -> result.withStatus(Status.SKIPPED));
        getLifecycle().stopStep();
        updateScenarioStatus(Status.SKIPPED);
    }

    @Override
    public void failed(final String step, final Throwable cause) {
        ResultsUtils.getStatus(cause).ifPresent(status ->
                getLifecycle().updateStep(result -> result.withStatus(status))
        );
        getLifecycle().stopStep();
        updateScenarioStatus(Status.FAILED);
    }

    public AllureLifecycle getLifecycle() {
        return lifecycle;
    }

    protected void updateScenarioStatus(final Status passed) {
        final String scenarioUuid = scenarios.get();
        max(scenarioStatusStorage.get(scenarioUuid), passed)
                .ifPresent(status -> scenarioStatusStorage.put(scenarioUuid, status));
    }

    private String md5(final String string) {
        return DatatypeConverter.printHexBinary(getMessageDigest()
                .digest(string.getBytes(StandardCharsets.UTF_8))
        );
    }

    private MessageDigest getMessageDigest() {
        try {
            return MessageDigest.getInstance(MD_5);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Could not find md5 hashing algorithm", e);
        }
    }

    private Optional<Status> max(final Status first, final Status second) {
        return Stream.of(first, second)
                .filter(Objects::nonNull)
                .min(Status::compareTo);
    }
}
