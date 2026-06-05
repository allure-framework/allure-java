# allure-awaitility

Awaitility condition listener integration for Allure Java.

## Coordinates

`io.qameta.allure:allure-awaitility`

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-awaitility")
}
```

## Use

Register `io.qameta.allure.awaitility.AllureAwaitilityListener` as an Awaitility condition listener:

```java
await()
        .conditionEvaluationListener(new AllureAwaitilityListener())
        .untilAsserted(() -> assertThat(service.isReady()).isTrue());
```

## Captured Data

- Await start, poll, satisfaction, timeout, and exception events.
- Poll attempts as nested Allure steps.
- Timing information rendered through `TemporalDuration`.
