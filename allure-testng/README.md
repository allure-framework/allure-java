# allure-testng

TestNG 7 listener integration for Allure Java.

## Coordinates

`io.qameta.allure:allure-testng`

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-testng")
}
```

## Use

The module exposes `io.qameta.allure.testng.AllureTestNg` as a TestNG listener through service loader metadata. You can also register it explicitly in TestNG XML or with `@Listeners`.

```java
@Listeners(AllureTestNg.class)
class MyTest {
}
```

## Captured Data

- TestNG suites, tests, classes, methods, configuration methods, and data providers.
- Allure scopes for suite and test fixtures.
- Labels, links, parameters, JavaDoc descriptions, status details, and test-plan filtering.
