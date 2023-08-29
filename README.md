[license]: http://www.apache.org/licenses/LICENSE-2.0 "Apache License 2.0"
[blog]: https://qameta.io/blog
[gitter]: https://gitter.im/allure-framework/allure-core
[gitter-ru]: https://gitter.im/allure-framework/allure-ru
[twitter]: https://twitter.com/QametaSoftware "Qameta Software"
[twitter-team]: https://twitter.com/QametaSoftware/lists/team/members "Team"

[CONTRIBUTING.md]: .github/CONTRIBUTING.md
[docs]: https://docs.qameta.io/allure/2.0/

# Allure Java Integrations 

[![Build](https://github.com/allure-framework/allure-java/actions/workflows/build.yml/badge.svg)](https://github.com/allure-framework/allure-java/actions/workflows/build.yml) 
[![Allure Java](https://img.shields.io/github/release/allure-framework/allure-java.svg)](https://github.com/allure-framework/allure-java/releases/latest)

The repository contains new versions of adaptors for JVM-based test frameworks.

All the artifacts are deployed to `https://repo1.maven.org/maven2/io/qameta/allure/`.

## TestNG

The new TestNG adaptors is pretty much ready. To use the adaptor you should add the following dependency:

```xml
<dependency>
   <groupId>io.qameta.allure</groupId>
   <artifactId>allure-testng</artifactId>
   <version>$LATEST_VERSION</version>
</dependency>
```

also you need to configure AspectJ weaver to support steps.

## JUnit 4

The first draft of a new JUnit 4 adaptor is ready. To use the adaptor you should add the following dependency:

```xml
<dependency>
   <groupId>io.qameta.allure</groupId>
   <artifactId>allure-junit4</artifactId>
   <version>$LATEST_VERSION</version>
</dependency>
```

## JUnit 5

To use JUnit 5 simply add the following dependency to your project:

```xml
<dependency>
   <groupId>io.qameta.allure</groupId>
   <artifactId>allure-junit5</artifactId>
   <version>$LATEST_VERSION</version>
</dependency>
```

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

## Cucumber
4,5,6,7 versions are supported instead of N use the required version.
To use Cucumber simply add the following dependency to your project:

[How to use allure cucumber code examples](https://github.com/allure-examples?q=cucumber&type=all&language=&sort=)
```xml
<dependency>
   <groupId>io.qameta.allure</groupId>
   <artifactId>allure-cucumberN-jvm</artifactId>
   <version>$LATEST_VERSION</version>
</dependency>

```