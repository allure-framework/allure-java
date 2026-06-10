# allure-okhttp3

OkHttp interceptor integration for Allure Java.

Use this module when your tests or test clients use OkHttp and you want request, response, and transport error details to appear in Allure Report.

## Supported Versions

- Allure Java 3.x requires Java 17 or newer.
- This module targets the OkHttp 3/4 `okhttp3` API.
- The current build validates against OkHttp 4.12.0.

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

## Report Output

- Request method, URL, headers, and body when available.
- Response status, message, headers, body, and timing.
- IOException details for failed exchanges.
- Redacted credentials and any custom redaction/truncation rules you configure.
