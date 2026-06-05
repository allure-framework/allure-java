# allure-citrus

Citrus listener integration for Allure Java.

## Coordinates

`io.qameta.allure:allure-citrus`

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-citrus")
}
```

## Use

Register `io.qameta.allure.citrus.AllureCitrus` with Citrus as a test, test suite, and test action listener. The listener translates Citrus suite, test case, and action events into Allure tests and steps.

## Captured Data

- Citrus test cases and suite lifecycle.
- Test actions as Allure steps.
- Standard Allure labels, links, parameters, status, and status details.
