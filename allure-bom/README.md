# allure-bom

Bill of materials for Allure Java artifacts.

## Coordinates

`io.qameta.allure:allure-bom`

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

## Use

Import the BOM once, then omit versions from individual Allure Java module dependencies. This keeps framework adapters, runtime APIs, and support modules on the same release line.
