package io.qameta.allure.cucumberjvm;

import gherkin.formatter.model.Scenario;

/**
 * Scenario Utils.
 */
@SuppressWarnings("PMD.DefaultPackage")
final class ScenarioUtils {

    private static final String UNSAFE_CHARACTERS_PATTERN = "[^0-9a-zA-Zа-яА-Я_\\-.]";

    private ScenarioUtils() { }

    /**
     * Generate safe UUID from Cucumber Scenario ID.
     * @param scenario Cucumber Scenario instance
     * @return generated UUID
     */
    /* default */ static String getScenarioUuid(final Scenario scenario) {
        return scenario.getId().replaceAll(UNSAFE_CHARACTERS_PATTERN, "_") + "-" + Utils.md5(scenario.getId());
    }

}
