# allure-karate

Karate runtime listener integration for Allure Java.

Use this module when your API tests run on Karate 2 and you want Karate features, scenarios, steps, tags, and runtime attachments to appear in Allure Report.

## Supported Versions

- This module targets Karate 2.x.
- The current build validates against `io.karatelabs:karate-core:2.0.10`.
- Karate 2.0.10 is built for Java 21, so this module requires Java 21 or newer.

## Installation

Gradle:

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-karate")
}
```

Maven, with `allure-bom` imported in dependency management:

```xml
<dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-karate</artifactId>
    <scope>test</scope>
</dependency>
```

## Setup

Register `io.qameta.allure.karate.AllureKarate` as a Karate runtime listener:

```java
Runner.builder()
        .path("classpath:features")
        .listener(new AllureKarate())
        .parallel(4);
```

## Report Output

- Karate features, scenarios, and steps.
- Tags mapped to Allure labels and links where supported.
- Runtime attachments produced by Karate steps.
