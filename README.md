[license]: http://www.apache.org/licenses/LICENSE-2.0 "Apache License 2.0"
[blog]: https://qameta.io/blog
[gitter]: https://gitter.im/allure-framework/allure-core
[gitter-ru]: https://gitter.im/allure-framework/allure-ru
[twitter]: https://twitter.com/QametaSoftware "Qameta Software"
[twitter-team]: https://twitter.com/QametaSoftware/lists/team/members "Team"

[CONTRIBUTING.md]: .github/CONTRIBUTING.md
[docs]: https://allurereport.org/docs/

# Allure Java Integrations 

[![Build](https://github.com/allure-framework/allure-java/actions/workflows/build.yml/badge.svg)](https://github.com/allure-framework/allure-java/actions/workflows/build.yml) 
[![Allure Java](https://img.shields.io/github/release/allure-framework/allure-java.svg)](https://github.com/allure-framework/allure-java/releases/latest)

> The repository contains new versions of adaptors for JVM-based test frameworks.

[<img src="https://allurereport.org/public/img/allure-report.svg" height="85px" alt="Allure Report logo" align="right" />](https://allurereport.org "Allure Report")

- Learn more about Allure Report at [https://allurereport.org](https://allurereport.org)
- 📚 [Documentation](https://allurereport.org/docs/) – discover official documentation for Allure Report
- ❓ [Questions and Support](https://github.com/orgs/allure-framework/discussions/categories/questions-support) – get help from the team and community
- 📢 [Official announcements](https://github.com/orgs/allure-framework/discussions/categories/announcements) –  stay updated with our latest news and updates
- 💬 [General Discussion](https://github.com/orgs/allure-framework/discussions/categories/general-discussion) – engage in casual conversations, share insights and ideas with the community
- 🖥️ [Live Demo](https://demo.allurereport.org/) — explore a live example of Allure Report in action

---
## TestNG

- 🚀 Documentation — https://allurereport.org/docs/testng/
- 📚 Example project — https://github.com/allure-examples?q=topic%3Atestng
- ✅ Generate a project in 10 seconds via Allure Start - https://allurereport.org/start/

## JUnit 4

- 🚀 Documentation — work in progress
- 📚 Example project — https://github.com/allure-examples?q=topic%3Ajunit4
- ✅ Generate a project in 10 seconds via Allure Start - https://allurereport.org/start/
- 
## JUnit Jupiter (JUnit 5 and 6)

- 🚀 Documentation — https://allurereport.org/docs/junit5/
- 📚 Example project — https://github.com/allure-examples?q=topic%3Ajunit5
- ✅ Generate a project in 10 seconds via Allure Start - https://allurereport.org/start/
- 🧩 Use `io.qameta.allure:allure-jupiter` for new setups. `allure-junit5` remains available as a deprecated compatibility alias during migration.

## Cucumber JVM

- 🚀 Documentation — https://allurereport.org/docs/cucumberjvm/
- 📚 Example project — https://github.com/allure-examples?q=cucumber&type=all&language=java
- ✅ Generate a project in 10 seconds via Allure Start - https://allurereport.org/start/

## Spock

- 🚀 Documentation — https://allurereport.org/docs/spock/
- 📚 Example project — https://github.com/allure-examples?q=topic%3Aspock
- ✅ Generate a project in 10 seconds via Allure Start - https://allurereport.org/start/
  
## Selenide

Listener for Selenide, that logging steps for Allure:

```xml
<dependency>
   <groupId>io.qameta.allure</groupId>
   <artifactId>allure-selenide</artifactId>
   <version>$LATEST_VERSION</version>
</dependency>
```

Usage example:
```
SelenideLogger.addListener("AllureSelenide", new AllureSelenide().screenshots(true).savePageSource(false));

Capture selenium logs:
SelenideLogger.addListener("AllureSelenide", new AllureSelenide().enableLogs(LogType.BROWSER, Level.ALL));
https://github.com/SeleniumHQ/selenium/wiki/Logging
```

## Playwright Java

AspectJ-based integration for Playwright Java that reports browser actions as Allure steps and attaches
Playwright screenshots automatically:

```xml
<dependency>
   <groupId>io.qameta.allure</groupId>
   <artifactId>allure-playwright</artifactId>
   <version>$LATEST_VERSION</version>
</dependency>
```

Enable the AspectJ weaver for automatic action steps:
```
-javaagent:/path/to/aspectjweaver.jar
```

Usage example with Playwright Java JUnit fixtures:
```java
@UsePlaywright
class UiTest {

    @Test
    void shouldOpenPage(Page page) {
        page.navigate("https://playwright.dev");
        page.screenshot();
    }
}
```

The module registers an Allure test lifecycle listener automatically, so per-test cleanup, failure diagnostics,
and final trace/log flush work with any test framework that reports through Allure. Playwright pages and
contexts are tracked by the AspectJ integration when they are created or used. Use
`AllurePlaywright.register(...)` only for pages or contexts the aspect cannot observe.

Frameworks or custom runners that do not use the Allure lifecycle can call the reporting hooks directly:
```java
AllurePlaywright.beforeTest();
try {
    testBody();
} catch (Throwable e) {
    AllurePlaywright.afterTestFailure(e);
    throw e;
} finally {
    AllurePlaywright.afterTest();
}
```

The following defaults can be overridden in `allure.properties`:
```
allure.playwright.steps.enabled=true
allure.playwright.steps.mode=actions
allure.playwright.parameters=redacted
allure.playwright.screenshots.attach=true
allure.playwright.failure.screenshot=true
allure.playwright.failure.page-source=true
allure.playwright.close.trace=true
allure.playwright.close.video=true
allure.playwright.close.page-logs=true
```


## Rest Assured

Filter for rest-assured http client, that generates attachment for allure.

```xml
<dependency>
   <groupId>io.qameta.allure</groupId>
   <artifactId>allure-rest-assured</artifactId>
   <version>$LATEST_VERSION</version>
</dependency>
```

Usage example:
```
.filter(new AllureRestAssured())
```
You can specify custom templates, which should be placed in src/main/resources/tpl folder:
```
.filter(new AllureRestAssured()
        .withRequestTemplate("custom-http-request.ftl")
        .withResponseTemplate("custom-http-response.ftl"))
```

## Spring Web

Interceptor for Spring synchronous HTTP clients, that generates attachments for allure.

```xml
<dependency>
   <groupId>io.qameta.allure</groupId>
   <artifactId>allure-spring-web</artifactId>
   <version>$LATEST_VERSION</version>
</dependency>
```

Usage example with `RestClient`:
```
RestClient restClient = RestClient.builder()
        .requestFactory(new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()))
        .requestInterceptor(new AllureRestTemplate())
        .build();
```
Use a buffering request factory when the client should still be able to read the response body after Allure captures it.

`RestTemplate` remains supported:
```
RestTemplate restTemplate = new RestTemplate(
        new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory())
);
restTemplate.setInterceptors(Collections.singletonList(new AllureRestTemplate()));
```

You can specify custom templates, which should be placed in src/main/resources/tpl folder:
```
new AllureRestTemplate()
        .setRequestTemplate("custom-http-request.ftl")
        .setResponseTemplate("custom-http-response.ftl")
```

## OkHttp

Interceptor for OkHttp client, that generates attachment for allure.

```xml
<dependency>
   <groupId>io.qameta.allure</groupId>
   <artifactId>allure-okhttp3</artifactId>
   <version>$LATEST_VERSION</version>
</dependency>
```

Usage example:
```
.addInterceptor(new AllureOkHttp3())
```
You can specify custom templates, which should be placed in src/main/resources/tpl folder:
```
.addInterceptor(new AllureOkHttp3()
                .withRequestTemplate("custom-http-request.ftl")
                .withResponseTemplate("custom-http-response.ftl"))

```

## gRPC

Interceptor for gRPC stubs, that generates attachment for allure.

```xml
<dependency>
   <groupId>io.qameta.allure</groupId>
   <artifactId>allure-grpc</artifactId>
   <version>$LATEST_VERSION</version>
</dependency>
```

Usage example:
```
.newBlockingStub(channel).withInterceptors(new AllureGrpc());
```
You can enable interception of response metadata (disabled by default)
```
.withInterceptors(new AllureGrpc()
                .interceptResponseMetadata(true))
```
By default, a step will be marked as failed in case that response contains any statuses except 0(OK).
You can change this behavior, for example, for negative scenarios
```
.withInterceptors(new AllureGrpc()
                .markStepFailedOnNonZeroCode(false))
```
You can specify custom templates, which should be placed in src/main/resources/tpl folder:
```
.withInterceptors(new AllureGrpc()
                .setRequestTemplate("custom-http-request.ftl")
                .setResponseTemplate("custom-http-response.ftl"))
```

## Http client

Interceptors for Apache HTTP client, that generates attachment for allure.

```xml
<dependency>
   <groupId>io.qameta.allure</groupId>
   <artifactId>allure-httpclient</artifactId>
   <version>$LATEST_VERSION</version>
</dependency>
```

Usage example:
```
.addInterceptorFirst(new AllureHttpClientRequest())
.addInterceptorLast(new AllureHttpClientResponse());
```

## Http client 5
Interceptors for Apache [httpclient5](https://hc.apache.org/httpcomponents-client-5.2.x/index.html). 
Additional info can be found in module `allure-httpclient5`

```xml
<dependency>
   <groupId>io.qameta.allure</groupId>
   <artifactId>allure-httpclient5</artifactId>
   <version>$LATEST_VERSION</version>
</dependency>
```

Usage example:
```java
final HttpClientBuilder builder = HttpClientBuilder.create()
        .addRequestInterceptorFirst(new AllureHttpClient5Request("your-request-template-attachment.ftl"))
        .addResponseInterceptorLast(new AllureHttpClient5Response("your-response-template-attachment.ftl"));
```

## JAX-RS Filter

Filter that can be used with JAX-RS compliant clients such as RESTeasy and Jersey

```xml
<dependency>
   <groupId>io.qameta.allure</groupId>
   <artifactId>allure-jax-rs</artifactId>
   <version>$LATEST_VERSION</version>
</dependency>
```

Usage example:
```
.register(AllureJaxRs.class)
```

## JsonUnit
JsonPatchMatcher is extension of JsonUnit matcher, that generates pretty html attachment for differences based on [json diff patch](https://github.com/benjamine/jsondiffpatch/blob/master/docs/deltas.md).

```xml
<dependency>
   <groupId>io.qameta.allure</groupId>
   <artifactId>allure-jsonunit</artifactId>
   <version>$LATEST_VERSION</version>
</dependency>
```

## Awaitility
Extended logging for poling and ignored exceptions for [awaitility](https://github.com/awaitility/awaitility). For 
more usage example look into module `allure-awaitility`

```xml
<dependency>
   <groupId>io.qameta.allure</groupId>
   <artifactId>allure-awaitility</artifactId>
   <version>$LATEST_VERSION</version>
</dependency>
```

Usage example:
```
Awaitility.setDefaultConditionEvaluationListener(new AllureAwaitilityListener());
```

