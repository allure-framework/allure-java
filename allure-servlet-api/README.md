# allure-servlet-api

Jakarta Servlet request and response conversion helpers for Allure Java.

Use this module when you are building a servlet-based test integration and need to convert Jakarta Servlet request or response objects into Allure HTTP request/response data.

## Supported Versions

- Allure Java 3.x requires Java 17 or newer.
- This module targets Jakarta Servlet.
- The current build validates against Jakarta Servlet API 6.1.0.

## Installation

Gradle:

```kotlin
dependencies {
    implementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    implementation("io.qameta.allure:allure-servlet-api")
}
```

Maven, with `allure-bom` imported in dependency management:

```xml
<dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-servlet-api</artifactId>
</dependency>
```

## Setup

Use `io.qameta.allure.servletapi.HttpServletAttachmentBuilder` when a servlet-based integration needs to convert Jakarta Servlet request or response objects.

```java
HttpExchangeRequest request = HttpServletAttachmentBuilder.buildRequest(servletRequest);
HttpExchangeResponse response = HttpServletAttachmentBuilder.buildResponse(servletResponse);
```

This module does not register a servlet filter by itself. Pair the converted data with `Allure.addHttpExchange(...)` from `allure-java-commons` when your integration is ready to write the exchange.

## Report Output

- Servlet request method, URL, headers, cookies, query data, and body when available.
- Servlet response status, headers, cookies, and body when available.
- Redaction and truncation can be applied through the HTTP exchange builder before writing the attachment.
