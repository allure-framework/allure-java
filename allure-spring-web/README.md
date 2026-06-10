# allure-spring-web

Spring Web client interceptor integration for Allure Java.

Use this module when your tests use Spring's synchronous HTTP client stack and you want request and response details to appear in Allure Report.

## Supported Versions

- Allure Java 3.x requires Java 17 or newer.
- This module targets Spring Web 7.x.
- The current build validates against Spring Web 7.0.8.

## Installation

Gradle:

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-spring-web")
}
```

Maven, with `allure-bom` imported in dependency management:

```xml
<dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-spring-web</artifactId>
    <scope>test</scope>
</dependency>
```

## Setup

Register `io.qameta.allure.springweb.AllureRestTemplate` with `RestTemplate` or another synchronous Spring HTTP client that accepts `ClientHttpRequestInterceptor`.

```java
RestTemplate restTemplate = new RestTemplate(
        new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory())
);
restTemplate.getInterceptors().add(new AllureRestTemplate()
        .configureHttpExchange(exchange -> exchange.redactHeader("Authorization")));
```

Configure a buffering request factory when caller code also needs to read the response body after interception.

## Report Output

- Request method, URI, headers, body, and timing.
- Response status, status text, headers, and body.
- Redacted credentials and any custom redaction/truncation rules you configure.
