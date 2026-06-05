# allure-grpc

gRPC client interceptor integration for Allure Java.

## Coordinates

`io.qameta.allure:allure-grpc`

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-grpc")
}
```

## Use

Attach `io.qameta.allure.grpc.AllureGrpc` to a gRPC channel or stub as a client interceptor.

```java
var channel = ManagedChannelBuilder.forAddress("localhost", 8080)
        .intercept(new AllureGrpc())
        .usePlaintext()
        .build();
```

## Captured Data

- gRPC method calls as Allure steps.
- Request and response messages, metadata, status, and timing.
- A single structured HTTP exchange attachment with `grpc` stream metadata.
