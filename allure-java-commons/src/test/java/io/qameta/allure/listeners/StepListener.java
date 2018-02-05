package io.qameta.allure.listeners;

import io.qameta.allure.listener.StepLifecycleListener;
import io.qameta.allure.model.StepResult;

import static io.qameta.allure.Allure.addAttachment;

/**
 * @author sskorol (Sergey Korol)
 */
public class StepListener implements StepLifecycleListener {

    @Override
    public void beforeStepStop(final StepResult result) {
        if (result.getName().equals("Execute dummy step")) {
            addAttachment("Log", "text content");
        }
    }
}
