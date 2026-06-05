# allure-karate

Karate runtime hook integration for Allure Java.

## Coordinates

`io.qameta.allure:allure-karate`

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-karate")
}
```

## Use

Register `io.qameta.allure.karate.AllureKarate` as a Karate runtime hook:

```java
Runner.path("classpath:features")
        .hook(new AllureKarate())
        .parallel(4);
```

## Captured Data

- Karate features, scenarios, and steps.
- Tags as Allure labels and links where supported.
- Runtime attachments produced by Karate steps.
