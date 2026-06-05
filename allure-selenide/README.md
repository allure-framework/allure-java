# allure-selenide

Selenide listener integration for Allure Java.

## Coordinates

`io.qameta.allure:allure-selenide`

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-selenide")
}
```

## Use

Register `io.qameta.allure.selenide.AllureSelenide` with `SelenideLogger`.

```java
SelenideLogger.addListener(
        "AllureSelenide",
        new AllureSelenide()
                .screenshots(true)
                .savePageSource(true)
);
```

## Captured Data

- Selenide log events as Allure steps.
- Screenshots and page source on failure when enabled.
- Browser logs when configured with `enableLogs`.
