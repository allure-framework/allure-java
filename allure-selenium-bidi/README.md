# allure-selenium-bidi

Selenium WebDriver BiDi listener integration for Allure Java.

Use this module when your Selenium 4 browser tests need browser log and network diagnostics collected through WebDriver BiDi and attached to Allure Report.

## Supported Versions

- Allure Java 3.x requires Java 17 or newer.
- This module targets Selenium WebDriver 4 with BiDi support.
- The current build validates against Selenium Java 4.44.0 and the matching docker-selenium 4.44.0-20260505 image.

## Installation

Gradle:

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-selenium-bidi")
}
```

Maven, with `allure-bom` imported in dependency management:

```xml
<dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-selenium-bidi</artifactId>
    <scope>test</scope>
</dependency>
```

## Setup

Decorate a Selenium `WebDriver` with `io.qameta.allure.seleniumbidi.AllureWebDriverBiDi`.

```java
AllureWebDriverBiDi bidi = new AllureWebDriverBiDi()
        .logs(true)
        .network(true)
        .maxLogEntries(500)
        .maxNetworkEvents(500)
        .redactHeaders("Authorization", "Cookie");

WebDriver driver = bidi.decorate(new ChromeDriver());
```

Close the listener at the end of the test or fixture when you manage it manually.

## Report Output

- Browser log events collected from WebDriver BiDi.
- Network events collected from WebDriver BiDi.
- Aggregated attachments with configurable limits and header redaction.
