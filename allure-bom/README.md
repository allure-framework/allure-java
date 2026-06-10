# allure-bom

Bill of materials for Allure Java artifacts.

Use this module to keep all Allure Java dependencies on the same release line. It is the recommended way to declare Allure modules in Gradle and Maven projects.

## Supported Versions

- Allure Java 3.x requires Java 17 or newer for runtime modules.
- The BOM aligns all artifacts published from the same Allure Java release.

## Installation

Gradle:

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-jupiter")
    testImplementation("io.qameta.allure:allure-rest-assured")
}
```

Maven:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.qameta.allure</groupId>
            <artifactId>allure-bom</artifactId>
            <version>${allure.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

After importing the BOM, omit versions from individual Allure Java dependencies.

## What It Aligns

- Test framework adapters such as `allure-jupiter`, `allure-testng`, and `allure-cucumber7-jvm`.
- Runtime and support APIs such as `allure-java-commons` and `allure-model`.
- HTTP, browser, assertion, and utility integrations.

Use one Allure version for all modules in a test suite. Mixing versions can produce missing metadata, duplicate lifecycle listeners, or unreadable result files.

## What To Expect

The BOM does not write Allure results by itself. It only controls dependency versions. Add at least one adapter, such as `allure-jupiter`, `allure-testng`, or `allure-cucumber7-jvm`, to produce report data.
