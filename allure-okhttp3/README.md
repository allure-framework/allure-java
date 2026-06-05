# allure-okhttp3

OkHttp 3 and 4 interceptor integration for Allure Java.

## Coordinates

`io.qameta.allure:allure-okhttp3`

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-okhttp3")
}
```

## Use

Register `io.qameta.allure.okhttp3.AllureOkHttp3` as an OkHttp interceptor.

```java
OkHttpClient client = new OkHttpClient.Builder()
        .addInterceptor(new AllureOkHttp3())
        .build();
```

## Captured Data

- Request method, URL, headers, and body when available.
- Response status, message, headers, body, and timing.
- IOException details for failed exchanges.
- A single structured HTTP exchange attachment.
