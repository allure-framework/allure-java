# allure-httpclient5

Apache HttpClient 5 interceptor integration for Allure Java.

## Coordinates

`io.qameta.allure:allure-httpclient5`

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-httpclient5")
}
```

## Use

Register the request and response interceptors on an HttpClient 5 builder:

```java
HttpClientBuilder.create()
        .addRequestInterceptorFirst(new AllureHttpClient5Request())
        .addResponseInterceptorLast(new AllureHttpClient5Response())
        .build();
```

## Captured Data

- Request method, URI, headers, and decompressed body when available.
- Response status, headers, body, and timing.
- A single structured HTTP exchange attachment.

## Notes

- Use `allure-httpclient` for Apache HttpClient 4.
- Register both interceptors together to produce a complete request/response exchange.
