# Allure Java

Allure Java is the JVM integration family for [Allure Report](https://allurereport.org/). It helps Java, Groovy, Scala, Kotlin, and other JVM test suites write Allure result files that can be viewed in Allure Report.

## Requirements

- Allure Java 3.x targets Java 17 and newer.
- Use one framework adapter per test runtime, for example `allure-jupiter`, `allure-testng`, or `allure-cucumber7-jvm`.
- Prefer `allure-bom` to keep Allure module versions aligned.

## Quick Start

Gradle:

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-jupiter")
}

tasks.test {
    useJUnitPlatform()
}
```

Maven:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.qameta.allure</groupId>
            <artifactId>allure-bom</artifactId>
            <version>${allure.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>io.qameta.allure</groupId>
        <artifactId>allure-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

Run your tests, then generate or serve the report from the produced Allure results directory, usually `build/allure-results` for Gradle or `target/allure-results` for Maven.

## Module Catalog

### Test Frameworks

| Module | Use When | Registration |
| --- | --- | --- |
| [`allure-jupiter`](allure-jupiter/README.md) | JUnit Jupiter on JUnit 5 or 6 | JUnit Platform service loader |
| [`allure-junit-platform`](allure-junit-platform/README.md) | Building a custom JUnit Platform integration | JUnit Platform listener and filter services |
| [`allure-junit4`](allure-junit4/README.md) | JUnit 4 runner where listeners can be configured | Register `io.qameta.allure.junit4.AllureJunit4` |
| [`allure-junit4-aspect`](allure-junit4-aspect/README.md) | Gradle built-in JUnit 4 execution | Enable AspectJ weaver |
| [`allure-testng`](allure-testng/README.md) | TestNG 7 suites | TestNG service loader or listener registration |
| [`allure-spock2`](allure-spock2/README.md) | Spock 2 specifications | Spock global extension service |
| [`allure-scalatest`](allure-scalatest/README.md) | ScalaTest suites | ScalaTest reporter |
| [`allure-cucumber7-jvm`](allure-cucumber7-jvm/README.md) | Cucumber JVM 7 | Cucumber plugin |
| [`allure-jbehave5`](allure-jbehave5/README.md) | JBehave 5 stories | JBehave story reporter |
| [`allure-karate`](allure-karate/README.md) | Karate runtime listeners | Karate runtime listener |
| [`allure-citrus`](allure-citrus/README.md) | Citrus tests | Citrus listeners |

### HTTP, Browser, And Client Integrations

| Module | Use When | Captured Data |
| --- | --- | --- |
| [`allure-rest-assured`](allure-rest-assured/README.md) | REST Assured filters | HTTP requests and responses |
| [`allure-httpclient5`](allure-httpclient5/README.md) | Apache HttpClient 5 interceptors | HTTP requests and responses |
| [`allure-httpclient`](allure-httpclient/README.md) | Apache HttpClient 4 interceptors | HTTP requests and responses |
| [`allure-okhttp3`](allure-okhttp3/README.md) | OkHttp 3 or 4 interceptors | HTTP requests and responses |
| [`allure-spring-web`](allure-spring-web/README.md) | Spring `RestTemplate` interceptors | HTTP requests and responses |
| [`allure-jax-rs`](allure-jax-rs/README.md) | Jakarta RESTful Web Services / JAX-RS client filters | HTTP requests and responses |
| [`allure-servlet-api`](allure-servlet-api/README.md) | Jakarta Servlet request/response conversion | Servlet request and response data |
| [`allure-grpc`](allure-grpc/README.md) | gRPC client interceptors | gRPC calls and metadata |
| [`allure-selenide`](allure-selenide/README.md) | Selenide UI tests | UI steps, screenshots, page source, logs |
| [`allure-selenium-bidi`](allure-selenium-bidi/README.md) | Selenium WebDriver BiDi sessions | Browser logs and network attachments |
| [`allure-playwright`](allure-playwright/README.md) | Playwright Java actions | AspectJ action steps and screenshots |
| [`allure-jooq`](allure-jooq/README.md) | jOOQ execution listener | SQL execution steps |

### Assertions And Utilities

| Module | Use When | Notes |
| --- | --- | --- |
| [`allure-assertj`](allure-assertj/README.md) | AssertJ assertions should appear as Allure steps | Requires AspectJ |
| [`allure-hamcrest`](allure-hamcrest/README.md) | Hamcrest assertions should appear as Allure steps | Requires AspectJ |
| [`allure-jupiter-assert`](allure-jupiter-assert/README.md) | JUnit Jupiter assertions should appear as Allure steps | Requires AspectJ |
| [`allure-awaitility`](allure-awaitility/README.md) | Awaitility polling should appear as Allure steps | Register condition listener |
| [`allure-jsonunit`](allure-jsonunit/README.md) | JsonUnit diffs should be attached to Allure | Provides JSON matcher/listener helpers |

### Core And Support Modules

| Module | Purpose |
| --- | --- |
| [`allure-bom`](allure-bom/README.md) | Maven/Gradle dependency alignment |
| [`allure-java-commons`](allure-java-commons/README.md) | Runtime API, lifecycle, annotations, aspects, and test-plan filtering |
| [`allure-model`](allure-model/README.md) | Serializable Allure result model |
| [`allure-descriptions-javadoc`](allure-descriptions-javadoc/README.md) | Annotation processor for JavaDoc-based test descriptions |

## Common Runtime APIs

Most adapters depend on `allure-java-commons`, which provides the high-level API:

```java
import io.qameta.allure.Allure;

Allure.step("Create order", () -> {
    Allure.attachment("request-id", "42");
});
```

For HTTP clients, browser tools, assertion libraries, and other integrations, add the matching module from the catalog above. Each module README shows the registration snippet for that tool.

## Resources

- [Allure Report documentation](https://allurereport.org/docs/)
