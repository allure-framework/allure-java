# allure-jooq

jOOQ execute listener integration for Allure Java.

## Coordinates

`io.qameta.allure:allure-jooq`

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-jooq")
}
```

## Use

Register `io.qameta.allure.jooq.AllureJooq` as a jOOQ `ExecuteListener`.

```java
var settings = new DefaultConfiguration()
        .set(new DefaultExecuteListenerProvider(new AllureJooq()));
```

## Captured Data

- SQL rendering and execution as Allure steps.
- Result records and execution failures.
- Step status and status details based on jOOQ execution events.
