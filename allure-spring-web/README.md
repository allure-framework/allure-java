# allure-spring-web

Spring Web client interceptor integration for Allure Java.

## Coordinates

`io.qameta.allure:allure-spring-web`

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-spring-web")
}
```

## Use

Register `io.qameta.allure.springweb.AllureRestTemplate` with `RestTemplate` or another synchronous Spring HTTP client that accepts `ClientHttpRequestInterceptor`.

```java
RestTemplate restTemplate = new RestTemplate(
        new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory())
);
restTemplate.getInterceptors().add(new AllureRestTemplate());
```

## Captured Data

- Request method, URI, headers, body, and timing.
- Response status, status text, headers, and body.
- A single structured HTTP exchange attachment.

## Notes

Configure a buffering request factory when caller code also needs to read the response body after interception.
