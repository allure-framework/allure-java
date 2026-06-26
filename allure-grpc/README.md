# allure-grpc

gRPC client interceptor integration for Allure Java.

Use this module when your tests call gRPC services and you want method calls, metadata, messages, timing, and statuses to appear in Allure Report.

## Supported Versions

- Allure Java 3.x requires Java 17 or newer.
- This module targets gRPC Java.
- The current build validates against gRPC Java 1.82.0 and Protobuf Java 4.35.1.

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

Request metadata capture is disabled by default. To enable request and response metadata capture
and apply redaction rules, use the advanced constructor:

```java
ClientInterceptor allure = new AllureGrpc(
        Allure.getLifecycle(),
        true,
        true,
        true,
        exchange -> exchange.redactHeader("authorization")
);
```

## Report Output

- gRPC method calls as Allure steps.
- Request and response messages, status, and timing.
- Optional request and response metadata when enabled through constructor flags.
- Stream metadata for unary and streaming calls where available.
