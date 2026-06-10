# allure-httpclient5

Apache HttpClient 5 interceptor integration for Allure Java.

Use this module when your tests or test clients use Apache HttpClient 5 and you want request and response details to appear in Allure Report.

## Supported Versions

- Allure Java 3.x requires Java 17 or newer.
- This module targets Apache HttpClient 5.x.
- The current build validates against Apache HttpClient 5.3.1.

## Installation

Gradle:

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-httpclient5")
}
```

Maven, with `allure-bom` imported in dependency management:

```xml
<dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-httpclient5</artifactId>
    <scope>test</scope>
</dependency>
```

## Setup

Register the request and response interceptors on an HttpClient 5 builder:

```java
AllureHttpClient5Response responseInterceptor = new AllureHttpClient5Response()
        .configureHttpExchange(exchange -> exchange.redactHeader("Authorization"));

CloseableHttpClient client = HttpClientBuilder.create()
        .addRequestInterceptorFirst(new AllureHttpClient5Request())
        .addResponseInterceptorLast(responseInterceptor)
        .build();
```

Register both interceptors together so Allure can combine request and response data from the same exchange.

## Report Output

- Request method, URI, headers, and decompressed body when available.
- Response status, headers, body, and timing.
- Redacted credentials and any custom redaction/truncation rules you configure.

Use `allure-httpclient` for Apache HttpClient 4.
