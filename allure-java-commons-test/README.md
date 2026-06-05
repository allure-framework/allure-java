# allure-java-commons-test

Internal test utilities for Allure Java modules.

## Coordinates

`io.qameta.allure:allure-java-commons-test`

## Use

This module is intended for tests inside the Allure Java repository and for adapter authors who need the same low-level fixtures. Normal Allure users should not depend on it.

## Provides

- In-memory Allure result writer stubs.
- Test result predicates and assertions.
- JUnit Platform helper runners for adapter tests.
- Random test data helpers used by Allure Java's own test suite.
