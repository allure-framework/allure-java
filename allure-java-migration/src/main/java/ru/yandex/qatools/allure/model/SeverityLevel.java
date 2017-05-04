package ru.yandex.qatools.allure.model;

/**
 * Severity level.
 */
public enum SeverityLevel {

    BLOCKER("blocker"),

    CRITICAL("critical"),

    NORMAL("normal"),

    MINOR("minor"),

    TRIVIAL("trivial");

    private final String value;

    SeverityLevel(final String v) {
        value = v;
    }

    public String value() {
        return value;
    }

}
