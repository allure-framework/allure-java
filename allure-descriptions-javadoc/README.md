# allure-descriptions-javadoc

Annotation processor that exposes JavaDoc comments as Allure descriptions.

Use this module when you want supported Allure adapters to use test method JavaDoc as the Allure description without repeating the same text in `@Description`.

## Supported Versions

- Allure Java 3.x requires Java 17 or newer.
- This module is a Java annotation processor and runs during compilation.
- It is consumed by adapters that read generated Allure description metadata.

## Installation

Gradle:

```kotlin
dependencies {
    testAnnotationProcessor("io.qameta.allure:allure-descriptions-javadoc:<allure-version>")
}
```

Maven:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>io.qameta.allure</groupId>
                <artifactId>allure-descriptions-javadoc</artifactId>
                <version>${allure.version}</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

## Setup

Add the processor to test compilation. Supported adapters can then read generated description metadata and use JavaDoc comments as Allure test descriptions when an explicit `@Description` is not present.

```java
/**
 * Verifies that paid orders can be refunded.
 */
@Test
void refundsPaidOrder() {
}
```

## Report Output

- Test descriptions derived from JavaDoc comments.
- Explicit Allure descriptions still take precedence when an adapter supports both sources.

This processor does not write Allure results by itself; it only makes JavaDoc text available to adapters at test runtime.
