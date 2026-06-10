# allure-jax-rs

Jakarta RESTful Web Services / JAX-RS client filter integration for Allure Java.

Use this module when your tests use a Jakarta RESTful Web Services client and you want request and response details to appear in Allure Report.

## Supported Versions

- Allure Java 3.x requires Java 17 or newer.
- This module targets the `jakarta.ws.rs` namespace.
- The current build validates against Jakarta RESTful Web Services API 4.0.0.

## Installation

Gradle:

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-jax-rs")
}
```

Maven, with `allure-bom` imported in dependency management:

```xml
<dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-jax-rs</artifactId>
    <scope>test</scope>
</dependency>
```

## Setup

Register `io.qameta.allure.jaxrs.AllureJaxRs` as both a client request and client response filter.

```java
Client client = ClientBuilder.newClient()
        .register(new AllureJaxRs()
                .configureHttpExchange(exchange -> exchange.redactHeader("Authorization")));
```

## Report Output

- Request method, URI, headers, and entity string.
- Response status, headers, body, and timing.
- Redacted credentials and any custom redaction/truncation rules you configure.

The artifact name remains `allure-jax-rs` because Jakarta RESTful Web Services still uses the JAX-RS terminology for the API and programming model.
