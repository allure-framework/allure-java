# allure-scalatest

ScalaTest reporter integration for Allure Java.

Use this module when your ScalaTest suites should produce Allure results with suites, tests, statuses, labels, links, parameters, and failure details.

## Supported Versions

- Allure Java 3.x requires Java 17 or newer.
- This module targets ScalaTest 3.2.x.
- The current build validates against ScalaTest 3.2.20.
- Artifacts are cross-built for Scala 2.12.21, Scala 2.13.18, and Scala 3.3.8 LTS.

## Installation

Use the artifact that matches your Scala binary version.

Gradle:

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-scalatest_3")
}
```

Use the `_2.12` or `_2.13` artifact instead when targeting the corresponding Scala 2 binary version.

sbt:

```scala
libraryDependencies += "io.qameta.allure" %% "allure-scalatest" % "<allure-version>" % Test
```

## Setup

Register `io.qameta.allure.scalatest.AllureScalatest` as a ScalaTest reporter.

Example sbt test option:

```scala
Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-C", "io.qameta.allure.scalatest.AllureScalatest")
```

## Report Output

- ScalaTest suites and tests.
- Passed, failed, canceled, ignored, and aborted events.
- Labels, links, parameters, status, and status details.
