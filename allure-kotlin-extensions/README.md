# allure-kotlin-extensions

A Kotlin-first, top-level facade for the public `Allure` runtime API. This module keeps Kotlin dependencies out of
`allure-java-commons` while providing named arguments, useful defaults, and unambiguous Kotlin lambdas.

## Supported Versions

- Allure Java 3.x requires Java 17 or newer.
- This module supports Kotlin 2.0 and newer.

## Installation

Gradle:

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-kotlin-extensions")
    testImplementation("io.qameta.allure:allure-jupiter") // or another test-framework adapter
}
```

Maven, with `allure-bom` imported in dependency management:

```xml
<dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-kotlin-extensions</artifactId>
    <scope>test</scope>
</dependency>
```

## Runtime Facade

Import individual functions or the complete facade:

```kotlin
import io.qameta.allure.kotlin.extensions.*
import io.qameta.allure.model.Parameter
import io.qameta.allure.model.Status
import io.qameta.allure.model.StatusDetails
```

The facade covers lifecycle access, completed and executable steps, stages, labels, parameters, links, descriptions,
global errors, synchronous and asynchronous attachments, and HTTP exchange attachments. It delegates to `Allure`, so
the behavior and current test context are shared with Java adapters and annotations. Executable steps expose a
Kotlin-native `StepScope` instead of the overloaded Java `Allure.StepContext`.

Java overload families are represented as Kotlin APIs instead of copied literally:

- the executable `Allure.step` SAM overloads become one receiver-lambda function
- the parameter overloads become one function with optional named arguments
- attachment content is the second argument and attachment metadata has defaults
- `getLifecycle` and `setLifecycle` become the `lifecycle` property

The original `Allure` class remains available for low-level interop.

## Steps And Stages

Import the top-level API instead of selecting between the Java `Allure.step` SAM overloads:

```kotlin
import io.qameta.allure.kotlin.extensions.step

val orderId = step("Create order") {
    parameter(name = "region", value = "eu-west")
    val id = createOrder()
    name("Create order $id")
    id
}
```

`StepScope.parameter` combines the Java overload family into one function. Its `excluded` and `mode` options can be
supplied independently with named arguments:

```kotlin
step("Authenticate") {
    parameter(
        name = "access token",
        value = token,
        excluded = true,
        mode = Parameter.Mode.MASKED,
    )
    authenticate(token)
}
```

The same function accepts a `Unit`-returning body. Omitting the name records the step as `step`:

```kotlin
step {
    verifyOrder(orderId)
}
```

Already-completed steps and semantic stages use the same names as the Java API:

```kotlin
stage("prepare data")
step("customer created")

stage("verify result")
step("optional check", Status.SKIPPED)
```

## Metadata

All runtime metadata operations support Kotlin named arguments:

```kotlin
epic("Checkout")
feature("Payment")
story("Saved card")
suite("Web checkout")
label(name = "component", value = "billing")

parameter(
    name = "access token",
    value = token,
    excluded = true,
    mode = Parameter.Mode.MASKED,
)

link(url = "https://example.test")
link(name = "Documentation", url = "https://example.test/docs")
link(name = "Dashboard", type = "dashboard", url = "https://example.test/dashboard")
issue(name = "BUG-42", url = "https://example.test/issues/42")
tms(name = "CASE-7", url = "https://example.test/cases/7")

description("**Markdown** description")
descriptionHtml("<p>HTML description</p>")
```

## Attachments

The attachment overloads put `content` second, provide useful defaults, and support named `type` and `fileExtension`
arguments:

```kotlin
import io.qameta.allure.kotlin.extensions.attachment

attachment(name = "response", content = responseBody)
attachment(
    name = "screenshot",
    content = screenshotBytes,
    type = "image/png",
    fileExtension = "png",
)
```

Supported content types are `String`, `ByteArray`, and `InputStream`. A `null` file extension lets Allure detect it
from the media type; an empty string suppresses the extension.

Asynchronous streams and captured HTTP exchanges are available through the same facade:

```kotlin
attachmentAsync(
    name = "worker output",
    content = outputFuture,
    type = "text/plain",
    fileExtension = "log",
)

addHttpExchange(name = "HTTP exchange", exchange = capturedExchange)
```

The future returned by `attachmentAsync` completes after the content is written. The owning executable also waits for
registered asynchronous attachments before it ends.

## Lifecycle And Run-Level Errors

Most tests do not need direct lifecycle access. Integrations can use the process-wide property when necessary:

```kotlin
val currentLifecycle = lifecycle
lifecycle = customLifecycle
```

Replacing the lifecycle affects the whole process and should be isolated from concurrent test execution.

Run-level failures that do not belong to a test or fixture can be reported from a throwable or explicit status
details:

```kotlin
globalError(startupFailure)
globalError(StatusDetails().setMessage("Environment unavailable"))
```

## Annotation And Coroutine Boundaries

These functions call the Allure lifecycle synchronously and do not require AspectJ. The `@Step` and `@Attachment`
annotations remain in `allure-java-commons` and still require weaving.

Avoid combining `@JvmOverloads` with `@Step` or `@Attachment` on methods with default parameters. Kotlin copies the
annotation to generated overloads, so AspectJ can record duplicate operations. Prefer regular Kotlin default arguments
when using these annotations.

This module does not add coroutine context propagation or a `suspend` step API. Add
`io.qameta.allure:allure-kotlin-coroutines` when a step body needs to suspend or Allure context must cross coroutine
dispatcher boundaries.
