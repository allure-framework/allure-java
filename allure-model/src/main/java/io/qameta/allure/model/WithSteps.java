package io.qameta.allure.model;

import java.util.List;

/**
 * @author charlie (Dmitry Baev).
 * @since 1.0-BETA1
 */
public interface WithSteps {

    List<StepResult> getSteps();

}
