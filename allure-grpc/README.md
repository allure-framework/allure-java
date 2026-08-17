# allure-grpc

gRPC client interceptor integration for Allure Java.

Use this module when your tests call gRPC services and you want method calls, metadata, messages, timing, and statuses to appear in Allure Report.

## Supported Versions

- Allure Java 3.x requires Java 17 or newer.
- This module targets gRPC Java.
- The current build validates against gRPC Java 1.83.1 and Protobuf Java 4.35.1.

## Installation

Gradle:

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-grpc")
}
```

Maven, with `allure-bom` imported in dependency management:

```xml
<dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-grpc</artifactId>
    <scope>test</scope>
</dependency>
```

## Setup

Attach `io.qameta.allure.grpc.AllureGrpc` to a gRPC channel or stub as a client interceptor.

```java
ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 8080)
        .intercept(new AllureGrpc())
        .usePlaintext()
        .build();
```

Request and response metadata are captured by default. Cookie metadata is represented as structured HTTP exchange
cookies, so header and cookie redaction can be configured through the HTTP exchange capture policy.

Use the builder to add application-specific header and cookie redaction:

```java
ClientInterceptor allure = AllureGrpc.builder()
        .redactHeader("x-api-key")
        .redactCookie("session")
        .build();
```

Metadata capture can be disabled independently for either direction:

```java
ClientInterceptor allure = AllureGrpc.builder()
        .captureRequestMetadata(false)
        .captureResponseMetadata(false)
        .build();
```

For other HTTP exchange capture options, configure the underlying exchange builder:

```java
ClientInterceptor allure = AllureGrpc.builder()
        .configureExchange(exchange -> exchange.setMaxBodySize(256_000))
        .build();
```

## Report Output

- gRPC method calls as Allure steps.
- Request and response messages, metadata, status, and timing.
- Repeated metadata values in their original order; binary metadata values are Base64-encoded.
- Stream metadata for unary and streaming calls where available.
