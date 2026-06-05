# allure-jax-rs

Jakarta RESTful Web Services / JAX-RS client filter integration for Allure Java.

## Coordinates

`io.qameta.allure:allure-jax-rs`

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-jax-rs")
}
```

## Use

Register `io.qameta.allure.jaxrs.AllureJaxRs` as both a client request and client response filter.

```java
ClientBuilder.newClient()
        .register(new AllureJaxRs());
```

## Captured Data

- Request method, URI, headers, and entity string.
- Response status, headers, body, and timing.
- A single structured HTTP exchange attachment.

## Notes

This module targets the `jakarta.ws.rs` namespace. The artifact name remains `allure-jax-rs` because Jakarta RESTful Web Services still uses the JAX-RS terminology for the API and programming model.
