# allure-okhttp3

OkHttp interceptor integration for Allure Java.

Use this module when your tests or test clients use OkHttp and you want request, response, and transport error details to appear in Allure Report.

## Supported Versions

- Allure Java 3.x requires Java 17 or newer.
- This module targets the OkHttp `okhttp3` API used by OkHttp 3, 4, and 5.
- The current build validates against OkHttp 5.5.0.

## Installation

Gradle:

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-okhttp3")
}
```

Maven, with `allure-bom` imported in dependency management:

```xml
<dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-okhttp3</artifactId>
    <scope>test</scope>
</dependency>
```

## Setup

Register `io.qameta.allure.okhttp3.AllureOkHttp3` as an OkHttp interceptor.

```java
OkHttpClient client = new OkHttpClient.Builder()
        .addInterceptor(new AllureOkHttp3()
                .configureHttpExchange(exchange -> exchange.redactHeader("Authorization")))
        .build();
```

## Server-Sent Events

OkHttp invokes server-sent event callbacks on reusable dispatcher threads. Use the Allure event-source factory so
steps and HTTP exchange attachments stay with the test or fixture that opens each source:

```java
EventSource.Factory eventSourceFactory = AllureEventSources.createFactory(client);

EventSource eventSource = eventSourceFactory.newEventSource(request, listener);
```

The returned factory is reusable. It captures context when `newEventSource` is called, so the client, factory, request,
and listener may all be constructed before the owning test or fixture starts. When no Allure test or fixture is
current, callbacks run without an Allure owner instead of inheriting stale context from an OkHttp dispatcher thread.

## Report Output

- Request method, URL, headers, and body when available.
- Response status, message, headers, body, and timing.
- IOException details for failed exchanges.
- Redacted credentials and any custom redaction/truncation rules you configure.
