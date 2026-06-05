# allure-selenium-bidi

Selenium WebDriver BiDi listener integration for Allure Java.

## Coordinates

`io.qameta.allure:allure-selenium-bidi`

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-selenium-bidi")
}
```

## Use

Decorate a Selenium `WebDriver` with `io.qameta.allure.seleniumbidi.AllureWebDriverBiDi`.

```java
AllureWebDriverBiDi bidi = new AllureWebDriverBiDi()
        .logs(true)
        .network(true);

WebDriver driver = bidi.decorate(new ChromeDriver());
```

Close the listener at the end of the test or fixture when you manage it manually.

## Captured Data

- Browser log events.
- Network events collected through WebDriver BiDi.
- Aggregated Allure attachments with configurable limits and header redaction.
