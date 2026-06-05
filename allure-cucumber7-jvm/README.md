# allure-cucumber7-jvm

Cucumber JVM 7 plugin for Allure Java.

## Coordinates

`io.qameta.allure:allure-cucumber7-jvm`

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-cucumber7-jvm")
}
```

## Use

Register the plugin with Cucumber:

```java
@CucumberOptions(plugin = "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm")
```

## Captured Data

- Features, scenarios, scenario outlines, examples, hooks, and steps.
- Cucumber attachments and write events.
- Allure labels, links, parameters, status, and status details from tags and annotations.
