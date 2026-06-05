# allure-jbehave5

JBehave 5 story reporter integration for Allure Java.

## Coordinates

`io.qameta.allure:allure-jbehave5`

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-jbehave5")
}
```

## Use

Register `io.qameta.allure.jbehave5.AllureJbehave5` as a JBehave story reporter in your JBehave 5 configuration.

## Captured Data

- Stories, scenarios, examples, and steps.
- Given stories and nested execution.
- Allure labels, parameters, status, and status details.
