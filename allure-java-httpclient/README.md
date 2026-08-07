# allure-java-httpclient

Java built-in HTTP Client integration for Allure Java.

Use this module to capture requests and responses sent through `java.net.http.HttpClient` as structured HTTP exchange attachments in Allure Report.

## Supported Versions

- Allure Java 3.x requires Java 17 or newer.
- The wrapped HTTP client API is available since Java 11.
- Synchronous requests, asynchronous requests, and accepted HTTP/2 push promises are supported.

## Installation

Gradle:

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-java-httpclient")
}
```

Maven, with `allure-bom` imported in dependency management:

```xml
<dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-java-httpclient</artifactId>
    <scope>test</scope>
</dependency>
```

## Setup

Wrap the client used by your tests with `io.qameta.allure.javahttpclient.AllureHttpClient`.

```java
HttpClient httpClient = AllureHttpClient.wrap(
        HttpClient.newBuilder().build()
);

HttpRequest request = HttpRequest.newBuilder(URI.create("https://example.test/api/items"))
        .GET()
        .build();

HttpResponse<String> response = httpClient.send(
        request,
        HttpResponse.BodyHandlers.ofString()
);
```

The wrapper passes calls through unchanged when no Allure test or fixture is running.

Customize redaction and body limits before sharing the client:

```java
HttpClient httpClient = AllureHttpClient.wrap(HttpClient.newHttpClient())
        .configureHttpExchange(exchange -> exchange
                .redactHeader("X-Api-Key")
                .redactQueryParameter("token")
                .setMaxBodySize(64 * 1024));
```

## Report Output

- One `HTTP exchange` attachment for each completed request or accepted push promise.
- Request method, URL, HTTP version, headers, and captured body.
- Response status, HTTP version, headers, and captured body.
- Transport errors when a request completes exceptionally.

Streaming response handlers remain streaming. The attachment contains the response bytes delivered by the time the response future completes; consume or close streaming bodies as required by `HttpClient`.
