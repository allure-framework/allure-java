# allure-spock

Legacy Spock 1 integration for Allure Java.

## Coordinates

`io.qameta.allure:allure-spock`

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-spock")
}
```

## Use

Add the dependency to a Spock 1 project. The module registers `io.qameta.allure.spock.AllureSpock` as a Spock global extension through service loader metadata.

## Notes

- Use this module only for the legacy Spock line.
- Prefer `allure-spock2` for Spock 2 projects.
- The extension records specifications, features, iterations, fixtures, labels, links, and errors.
