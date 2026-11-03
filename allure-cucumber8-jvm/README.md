# allure-cucumber8-jvm

Cucumber JVM 8 plugin for Allure Java.

Use this module when your BDD tests run on Cucumber JVM 8 and you want features, scenarios, hooks, steps, tags, attachments, and examples to appear in Allure Report.

This adapter mirrors `allure-cucumber7-jvm` and reads Gherkin line numbers from the Cucumber 8 messages API. `Location.getLine()` is a Java `int` there. Bytecode compiled against the cucumber 7 `Long getLine()` descriptor throws `NoSuchMethodError` on this runtime.

## Supported Versions

- Allure Java 3.x requires Java 17 or newer.
- This module targets Cucumber JVM 8.x.
- The test suite validates against Cucumber JVM 8.0.4.

## Installation

Gradle:

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-cucumber8-jvm")
}
```

Maven, with `allure-bom` imported in dependency management:

```xml
<dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-cucumber8-jvm</artifactId>
    <scope>test</scope>
</dependency>
```

## Setup

Register the plugin with Cucumber:

```java
@CucumberOptions(plugin = "io.qameta.allure.cucumber8jvm.AllureCucumber8Jvm")
```

You can also pass the plugin through your Cucumber runner or build tool configuration.

## Report Output

- Features, scenarios, scenario outlines, examples, hooks, and steps.
- Cucumber attachments and write events.
- Labels, links, parameters, status, and status details from tags and annotations.
