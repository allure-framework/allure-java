# allure-jupiter

JUnit Jupiter adapter for Allure Java.

## Coordinates

`io.qameta.allure:allure-jupiter`

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-jupiter")
}

tasks.test {
    useJUnitPlatform()
}
```

## Use

Add the dependency to a JUnit Jupiter project. The module contributes the Allure JUnit Platform listener and the Jupiter extension through service loader metadata.

## Captured Data

- JUnit Jupiter tests, fixtures, dynamic tests, repeated tests, and parameterized tests.
- Allure labels, links, descriptions, JavaDoc descriptions, parameters, and status details.
- Fixture start, stop, and failure metadata through the Jupiter extension.

## Notes

- Use this artifact for JUnit 5 and JUnit 6 Jupiter tests.
- The old `allure-junit5` alias is removed in Allure Java 3.x.
- Add `allure-jupiter-assert` only when JUnit assertions should be reported as nested Allure steps.
