# allure-rest-assured

REST Assured filter integration for Allure Java.

Use this module when your API tests use REST Assured and you want each request and response to be visible near the step that produced it in Allure Report.

## Supported Versions

- Allure Java 3.x requires Java 17 or newer.
- This module targets REST Assured 6.x.
- The current build validates against REST Assured 6.0.0.

## Installation

Gradle:

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-rest-assured")
}
```

Maven, with `allure-bom` imported in dependency management:

```xml
<dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-rest-assured</artifactId>
    <scope>test</scope>
</dependency>
```

## Setup

Register `io.qameta.allure.restassured.AllureRestAssured` as a REST Assured filter.

```java
given()
        .filter(new AllureRestAssured())
        .when()
        .get("/orders");
```

You can customize sensitive data handling before registering the filter:

```java
AllureRestAssured allure = new AllureRestAssured()
        .configureHttpExchange(exchange -> exchange
                .redactHeader("X-Api-Key")
                .redactCookie("SESSION")
                .setMaxBodySize(64 * 1024));
```

## Report Output

- Request method, URL, headers, cookies, form parameters, and body.
- Response status, status text, headers, body, and timing.
- Redacted REST Assured blacklisted headers and any custom redaction rules you configure.
