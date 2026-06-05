# allure-descriptions-javadoc

Annotation processor that exposes JavaDoc comments as Allure descriptions.

## Coordinates

`io.qameta.allure:allure-descriptions-javadoc`

```kotlin
dependencies {
    testAnnotationProcessor("io.qameta.allure:allure-descriptions-javadoc:<allure-version>")
}
```

## Use

Add the processor to test compilation. Supported adapters can then read generated description metadata and use JavaDoc comments as Allure test descriptions when an explicit `@Description` is not present.

## Notes

- This is a compile-time processor, not a runtime listener.
- It is normally added by Allure Java's own module build for adapter tests.
- Exclude it from modules where an annotation processor would create unwanted transitive dependency pressure.
