# allure-selenide

Selenide listener integration for Allure Java.

Use this module when your UI tests use Selenide and you want Selenide actions, screenshots, page source, and browser logs to appear in Allure Report.

## Supported Versions

- Allure Java 3.x requires Java 17 or newer.
- This module targets Selenide 7.x.
- The current build validates against Selenide 7.4.1.

## Installation

Gradle:

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-selenide")
}
```

Maven, with `allure-bom` imported in dependency management:

```xml
<dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-selenide</artifactId>
    <scope>test</scope>
</dependency>
```

## Setup

Register `io.qameta.allure.selenide.AllureSelenide` with `SelenideLogger`, usually in a test fixture before browser actions start.

```java
SelenideLogger.addListener(
        "AllureSelenide",
        new AllureSelenide()
                .screenshots(true)
                .savePageSource(true)
);
```

You can also enable browser log capture for a specific Selenium log type:

```java
new AllureSelenide()
        .enableLogs(LogType.BROWSER, Level.SEVERE);
```

## Report Output

- Selenide log events as Allure steps.
- Screenshots and page source on failure when enabled.
- Browser logs when configured with `enableLogs`.
