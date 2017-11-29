package io.qameta.allure.selenide;

import com.codeborne.selenide.impl.ScreenShotLaboratory;
import com.codeborne.selenide.logevents.LogEvent;
import com.codeborne.selenide.logevents.LogEventListener;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.util.ResultsUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * @author Artem Eroshenko.
 */
public class AllureSelenide implements LogEventListener {

    private final AllureLifecycle lifecycle;

    public AllureSelenide() {
        this(Allure.getLifecycle());
    }

    public AllureSelenide(final AllureLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    @Override
    public void onEvent(final LogEvent event) {
        lifecycle.getCurrentTestCase().ifPresent(uuid -> {
            final String stepUUID = UUID.randomUUID().toString();
            final long stepStopTime = System.currentTimeMillis();
            final long stepStartTime = stepStopTime - event.getDuration();

            lifecycle.startStep(stepUUID, new StepResult()
                    .withName(event.toString())
                    .withStatus(Status.PASSED)
                    .withStart(stepStartTime)
                    .withStop(stepStopTime));

            if (LogEvent.EventStatus.FAIL.equals(event.getStatus())) {
                final byte[] screenshotBytes = getScreenshot();
                lifecycle.addAttachment("Screenshot", "image/png", "png", screenshotBytes);
                lifecycle.updateStep(stepResult -> {
                    final StatusDetails details = ResultsUtils.getStatusDetails(event.getError())
                            .orElse(new StatusDetails());
                    stepResult.setStatus(Status.FAILED);
                    stepResult.setStatusDetails(details);
                });
            }
            lifecycle.stopStep(stepUUID);
        });
    }


    private byte[] getScreenshot() {
        final File file = new ScreenShotLaboratory().takeScreenShotAsFile();
        try {
            return FileUtils.readFileToByteArray(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            FileUtils.deleteQuietly(file);
        }
    }
}
