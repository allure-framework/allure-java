# allure-kotlin-coroutines

Coroutine-aware extensions for the Allure Java runtime API. This module builds on `allure-kotlin-extensions` and
keeps Kotlin coroutine dependencies out of `allure-java-commons`.

## Supported Versions

- Allure Java 3.x requires Java 17 or newer.
- This module supports Kotlin 2.0 and newer.
- The dependency baseline is `kotlinx-coroutines-core` 1.9.0; applications may align it to a newer compatible version.

## Installation

Gradle:

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-kotlin-coroutines")
    testImplementation("io.qameta.allure:allure-jupiter") // or another test-framework adapter
}
```

Maven, with `allure-bom` imported in dependency management:

```xml
<dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-kotlin-coroutines</artifactId>
    <scope>test</scope>
</dependency>
```

`allure-kotlin-coroutines` transitively includes `allure-kotlin-extensions`.

## Suspend Steps

Use the coroutine package when a step body needs to suspend:

```kotlin
import io.qameta.allure.kotlin.coroutines.step

val order = step("Load order") {
    val loaded = withContext(Dispatchers.IO) {
        orderClient.load()
    }
    parameter("orderId", loaded.id)
    name("Load order ${loaded.id}")
    loaded
}
```

The suspend function uses the same Kotlin-native `StepScope` as `allure-kotlin-extensions`, including named
`parameter` options. The step remains current when the coroutine changes threads. Nested synchronous steps and
attachments are therefore recorded beneath the suspend step. The function also supports `Unit` bodies and the default
name `step`.

Cancellation is never swallowed. A cancelled step is stopped, marked `broken` using the standard Allure throwable
mapping, and the cancellation exception is rethrown.

## Propagating An Existing Allure Context

Use `withAllureContext` to capture the current test, fixture, or step before launching structured coroutine work:

```kotlin
import io.qameta.allure.kotlin.coroutines.withAllureContext
import io.qameta.allure.kotlin.extensions.attachment

withAllureContext {
    coroutineScope {
        launch(Dispatchers.IO) {
            attachment(name = "worker output", content = loadOutput())
        }
        launch(Dispatchers.Default) {
            step("calculate summary") {
                calculateSummary()
            }
        }
    }
}
```

The capture happens when `withAllureContext` is called. Child coroutines receive isolated local execution stacks, so
parallel steps are siblings rather than accidentally nesting inside one another. The context restores each worker
thread after suspension or completion. If no Allure executable is current at capture time, that absence is propagated
too, preventing a coroutine from inheriting an unrelated Allure context already present on a worker.

Pass a non-global lifecycle as `withAllureContext(lifecycle) { ... }`.

For direct context composition, use `allureContext()` or `lifecycle.asCoroutineContext()`:

```kotlin
launch(Dispatchers.IO + allureContext()) {
    // ...
}
```

## Boundaries

- Keep coroutine work structured: the owning test, fixture, or step must remain active until its children complete.
- Do not use `GlobalScope` or another unstructured launch for report work that can outlive its owner.
- A manually started ambient `AllureLifecycle` step or stage must not remain open across a suspension. Use the
  coroutine `step` function for any operation whose body can suspend.
- `@Step` does not become coroutine-aware by adding this module. Annotated suspend functions are still woven as JVM
  methods and do not provide coroutine-context propagation; use the suspend step DSL instead.
