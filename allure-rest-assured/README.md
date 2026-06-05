# allure-rest-assured

REST Assured filter integration for Allure Java.

## Coordinates

`io.qameta.allure:allure-rest-assured`

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-rest-assured")
}
```

## Use

Register `io.qameta.allure.restassured.AllureRestAssured` as a REST Assured filter.

```java
given()
        .filter(new AllureRestAssured())
        .when()
        .get("/orders");
```

## Captured Data

- Request method, URL, headers, cookies, form parameters, and body.
- Response status, status text, headers, body, and timing.
- REST Assured blacklisted headers are redacted.
- A single structured HTTP exchange attachment.
