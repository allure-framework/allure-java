# allure-model

Serializable Allure result model for Java integrations.

## Coordinates

`io.qameta.allure:allure-model`

## Use

Most users receive this module transitively from `allure-java-commons`. Depend on it directly only when building tooling that reads, writes, or transforms Allure result data.

## Provides

- `TestResult`, `StepResult`, `FixtureResult`, and `ScopeResult`.
- Attachments, labels, links, parameters, statuses, stages, and status details.
- Common model interfaces for attachments, steps, parameters, links, and statuses.
