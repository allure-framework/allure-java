# allure-playwright

Playwright Java integration for Allure Java.

Use this module when your Playwright Java tests need Playwright actions, screenshots, page source, traces, videos, console messages, and page errors in Allure Report.

## Supported Versions

- Allure Java 3.x requires Java 17 or newer.
- This module targets Playwright Java.
- The current build validates against Playwright Java 1.59.0 and AspectJ 1.9.25.1.

## Installation

Gradle:

```kotlin
val aspectjAgent by configurations.creating

dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-playwright")
    testRuntimeOnly("org.aspectj:aspectjrt:<aspectj-version>")
    aspectjAgent("org.aspectj:aspectjweaver:<aspectj-version>")
}

tasks.test {
    doFirst {
        jvmArgs("-javaagent:${aspectjAgent.singleFile}")
    }
}
```

Maven, with `allure-bom` imported in dependency management, can use the same artifact together with an AspectJ javaagent configured for the test JVM.

## Setup

Enable the AspectJ weaver for automatic Playwright action steps. Register pages or browser contexts when you want failure diagnostics attached automatically.

```java
AllurePlaywright.register(page);
AllurePlaywright.attachScreenshot("Checkout page", page);
```

For trace capture, start tracing through the helper and close the returned session when the scenario ends:

```java
try (TraceSession trace = AllurePlaywright.startTracing(context)) {
    // run browser actions
}
```

## Report Output

- Playwright actions as Allure steps when AspectJ weaving is enabled.
- Screenshots, page source, traces, videos, console messages, and page errors.
- Failure diagnostics for registered pages and browser contexts.
