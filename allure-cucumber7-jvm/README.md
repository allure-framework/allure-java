# allure-cucumber7-jvm

Cucumber JVM 7 plugin for Allure Java.

Use this module when your BDD tests run on Cucumber JVM 7 and you want features, scenarios, hooks, steps, tags, attachments, and examples to appear in Allure Report.

## Supported Versions

- Allure Java 3.x requires Java 17 or newer.
- This module targets Cucumber JVM 7.x.
- The current build validates against Cucumber JVM 7.34.3 and Gherkin 36.1.0.

## Installation

Gradle:

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-cucumber7-jvm")
}
```

Maven, with `allure-bom` imported in dependency management:

```xml
<dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-cucumber7-jvm</artifactId>
    <scope>test</scope>
</dependency>
```

## Setup

Register the plugin with Cucumber:

```java
@CucumberOptions(plugin = "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm")
```

You can also pass the plugin through your Cucumber runner or build tool configuration.

## Report Output

- Features, scenarios, scenario outlines, examples, hooks, and steps.
- Cucumber attachments and write events.
- Labels, links, parameters, status, and status details from tags and annotations.
