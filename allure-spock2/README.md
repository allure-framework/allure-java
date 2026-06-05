# allure-spock2

Spock 2 integration for Allure Java.

## Coordinates

`io.qameta.allure:allure-spock2`

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-spock2")
}
```

## Use

Add the dependency to a Spock 2 project. The module registers `io.qameta.allure.spock2.AllureSpock2` as a Spock global extension through service loader metadata.

## Captured Data

- Specifications, features, iterations, fixture methods, and errors.
- Data-driven parameters and Spock tags.
- Allure labels, links, JavaDoc descriptions, test-plan filtering, and fixture metadata.
