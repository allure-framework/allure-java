# allure-httpclient

Apache HttpClient 4 interceptor integration for Allure Java.

Use this module when your tests or test clients use Apache HttpClient 4 and you want request and response details to appear in Allure Report.

## Supported Versions

- Allure Java 3.x requires Java 17 or newer.
- This module targets Apache HttpClient 4.x.
- The current build validates against Apache HttpClient 4.5.14.

## Installation

Gradle:

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-httpclient")
}
```

Maven, with `allure-bom` imported in dependency management:

```xml
<dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-httpclient</artifactId>
    <scope>test</scope>
</dependency>
```

## Setup

Register the request and response interceptors on an HttpClient 4 builder:

```java
AllureHttpClientResponse responseInterceptor = new AllureHttpClientResponse()
        .configureHttpExchange(exchange -> exchange.redactHeader("Authorization"));

CloseableHttpClient client = HttpClientBuilder.create()
        .addInterceptorFirst(new AllureHttpClientRequest())
        .addInterceptorLast(responseInterceptor)
        .build();
```

Register both interceptors together so Allure can combine request and response data from the same exchange.

## Report Output

- Request method, URI, headers, and body when available.
- Response status, headers, body, and timing.
- Redacted credentials and any custom redaction/truncation rules you configure.

Use `allure-httpclient5` for Apache HttpClient 5.
