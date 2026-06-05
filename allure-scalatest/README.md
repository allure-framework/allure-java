# allure-scalatest

ScalaTest reporter integration for Allure Java.

## Coordinates

`io.qameta.allure:allure-scalatest`

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-scalatest")
}
```

## Use

Register `io.qameta.allure.scalatest.AllureScalatest` as a ScalaTest reporter.

Example sbt test option:

```scala
Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-C", "io.qameta.allure.scalatest.AllureScalatest")
```

## Captured Data

- ScalaTest suites and tests.
- Passed, failed, canceled, ignored, and aborted events.
- Allure labels, links, parameters, and status details.
