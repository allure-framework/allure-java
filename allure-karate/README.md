# allure-karate

Karate runtime listener integration for Allure Java.

## Coordinates

`io.qameta.allure:allure-karate`

This module integrates with Karate 2.x and requires Java 21 or later.

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-karate")
}
```

## Use

Register `io.qameta.allure.karate.AllureKarate` as a Karate runtime listener:

```java
Runner.builder()
        .path("classpath:features")
        .listener(new AllureKarate())
        .parallel(4);
```

## Captured Data

- Karate features, scenarios, and steps.
- Tags as Allure labels and links where supported.
- Runtime attachments produced by Karate steps.
