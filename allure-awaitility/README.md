# allure-awaitility

Awaitility condition listener integration for Allure Java.

Use this module when your tests wait with Awaitility and you want polling attempts, timeouts, ignored exceptions, and successful waits to be visible in Allure Report.

## Supported Versions

- Allure Java 3.x requires Java 17 or newer.
- This module targets Awaitility 4.x.
- The current build validates against Awaitility 4.3.0.

## Installation

Gradle:

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-awaitility")
}
```

Maven, with `allure-bom` imported in dependency management:

```xml
<dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-awaitility</artifactId>
    <scope>test</scope>
</dependency>
```

## Setup

Register `io.qameta.allure.awaitility.AllureAwaitilityListener` as an Awaitility condition listener.

```java
await()
        .conditionEvaluationListener(new AllureAwaitilityListener())
        .untilAsserted(() -> assertThat(service.isReady()).isTrue());
```

You can also set it as the default listener for your test suite:

```java
Awaitility.setDefaultConditionEvaluationListener(new AllureAwaitilityListener());
```

## Report Output

- Await start, poll, satisfaction, timeout, and exception events.
- Poll attempts as nested Allure steps.
- Timing information rendered through `TemporalDuration`.
