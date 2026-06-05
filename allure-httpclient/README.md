# allure-httpclient

Apache HttpClient 4 interceptor integration for Allure Java.

## Coordinates

`io.qameta.allure:allure-httpclient`

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-httpclient")
}
```

## Use

Register the request and response interceptors on an HttpClient 4 builder:

```java
HttpClientBuilder.create()
        .addInterceptorFirst(new AllureHttpClientRequest())
        .addInterceptorLast(new AllureHttpClientResponse())
        .build();
```

## Captured Data

- Request method, URI, headers, and body when available.
- Response status, headers, body, and timing.
- A single structured HTTP exchange attachment.
