# allure-java-commons

Core runtime API for Allure Java.

Most Allure Report users receive this module transitively from a framework adapter such as `allure-jupiter` or `allure-testng`. Add it directly when you use the runtime API without a framework adapter or when you build a custom integration.

## Supported Versions

- Allure Java 3.x requires Java 17 or newer.
- The runtime API is shared by all Allure Java adapters in the same release line.
- The module bundles the JSON support it needs; users do not need to add Jackson for Allure result serialization.

## Installation

Gradle:

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-java-commons")
}
```

Maven, with `allure-bom` imported in dependency management:

```xml
<dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-java-commons</artifactId>
    <scope>test</scope>
</dependency>
```

## Runtime API

Use `io.qameta.allure.Allure` for high-level steps, attachments, labels, links, and descriptions.

```java
import io.qameta.allure.Allure;

Allure.step("Create order", () -> {
    Allure.addAttachment("request-id", "42");
});
```

High-level attachment APIs create attachment steps so attachments stay ordered with surrounding steps in Allure Report.

## Annotation API

The module provides annotations such as `@Step`, `@Attachment`, `@Owner`, `@Epic`, `@Feature`, and `@Story`.

Annotations that need method interception, such as `@Step` and `@Attachment`, require the AspectJ weaver in the test JVM. Framework-specific adapters may also consume metadata annotations directly.

## HTTP Exchange API

Use `io.qameta.allure.http.HttpExchange` when a custom integration needs to attach HTTP request and response details.

```java
HttpExchange exchange = HttpExchange.builder()
        .redactHeader("X-Api-Key")
        .redactCookie("SESSION")
        .redactQueryParameter("token")
        .redactFormParameter("password")
        .setMaxBodySize(64 * 1024)
        .request("POST", "https://example.test/orders", request -> request
                .addHeader("X-Api-Key", "secret")
                .addQuery("token", "abc")
                .setBody(HttpExchangeBody.utf8("password=secret")))
        .response(response -> response
                .setStatus(200))
        .build();

Allure.addHttpExchange("HTTP exchange", exchange);
```

The builder applies redaction and truncation before the exchange is attached. Common credential headers such as `Authorization`, `Proxy-Authorization`, `Cookie`, and `Set-Cookie` are redacted by default.

## Provides

- `Allure` high-level runtime API.
- `AllureLifecycle` for custom adapters and advanced result-writing workflows.
- Lifecycle listener interfaces.
- File-system result writer utilities.
- HTTP request/response capture model.
- Test-plan reader and selection helpers in `io.qameta.allure.testfilter`.

## What To Expect

This module writes result data only when your code or an adapter calls the runtime API. In ordinary test suites, add a framework adapter first and use `Allure.step(...)`, `Allure.addAttachment(...)`, and metadata annotations for extra report detail.
