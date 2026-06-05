# allure-java-commons

Core runtime API for Allure Java.

## Coordinates

`io.qameta.allure:allure-java-commons`

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-java-commons")
}
```

## Use

Most users receive this module transitively from a framework adapter. Depend on it directly when writing a custom integration or when using the runtime API without a framework adapter.

```java
Allure.step("Create order", () -> {
    Allure.attachment("request-id", "42");
});
```

## Provides

- `Allure` high-level runtime API.
- `AllureLifecycle` and lifecycle listener interfaces.
- Allure annotations such as `@Step`, `@Attachment`, `@Owner`, `@Epic`, `@Feature`, and `@Story`.
- File-system result writer utilities.
- Structured HTTP exchange model and serializer.
- Test-plan reader and selection helpers in `io.qameta.allure.testfilter`.

## HTTP Exchange Attachments

`io.qameta.allure.http` provides the public model for Allure 3 HTTP exchange attachments. Use
`HttpExchange.builder()` to create a redacted and truncated exchange, then `Allure.addHttpExchange(...)` to
serialize it as UTF-8 JSON with content type `application/vnd.allure.http+json`
and file extension `.httpexchange`.

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

`HttpExchangeSerializer` uses the Jackson runtime bundled into `allure-java-commons`; consumers do not need to
add Jackson directly. The serializer writes the exchange it receives and does not apply redaction itself. The builder
redacts common credential headers such as `Authorization`, `Proxy-Authorization`, `Cookie`, and `Set-Cookie` by
default, and truncates body payloads over 1 MiB.

High-level attachment APIs, including `Allure.addAttachment(...)`, async attachment helpers, and
`Allure.addHttpExchange(...)`, create attachment meta-steps so attachments stay ordered with surrounding steps.
Low-level `AllureLifecycle.addAttachment(...)` remains a direct write API for adapter internals.
