# allure-jsonunit

JsonUnit diff attachment integration for Allure Java.

## Coordinates

`io.qameta.allure:allure-jsonunit`

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-jsonunit")
}
```

## Use

Use `JsonPatchMatcher.jsonEquals(...)` when comparing JSON payloads.

```java
assertThat(actualJson, JsonPatchMatcher.jsonEquals(expectedJson));
```

## Captured Data

- JsonUnit comparison differences.
- A rendered JSON diff attachment named `JSON difference`.

## Notes

This module still uses module-local HTML rendering for JSON diffs. It is intentionally separate from the HTTP exchange attachment model.
