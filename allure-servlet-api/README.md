# allure-servlet-api

Jakarta Servlet request and response conversion helpers for Allure Java.

## Coordinates

`io.qameta.allure:allure-servlet-api`

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-servlet-api")
}
```

## Use

Use `io.qameta.allure.servletapi.HttpServletAttachmentBuilder` when a servlet-based integration needs to convert Jakarta Servlet request or response objects into the shared HTTP exchange model.

```java
HttpExchangeRequest request = HttpServletAttachmentBuilder.buildRequest(servletRequest);
HttpExchangeResponse response = HttpServletAttachmentBuilder.buildResponse(servletResponse);
```

## Notes

- This module targets Jakarta Servlet.
- It does not register a servlet filter by itself.
- Use `Allure.addHttpExchange(...)` from `allure-java-commons` to write a complete exchange attachment.
