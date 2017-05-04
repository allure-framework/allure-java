package ru.yandex.qatools.allure.model;

/**
 * Description type.
 */
public enum DescriptionType {

    MARKDOWN("markdown"),

    TEXT("text"),

    HTML("html");

    private final String value;

    DescriptionType(final String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

}
