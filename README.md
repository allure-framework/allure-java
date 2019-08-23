[license]: http://www.apache.org/licenses/LICENSE-2.0 "Apache License 2.0"
[blog]: https://qameta.io/blog
[gitter]: https://gitter.im/allure-framework/allure-core
[gitter-ru]: https://gitter.im/allure-framework/allure-ru
[twitter]: https://twitter.com/QametaSoftware "Qameta Software"
[twitter-team]: https://twitter.com/QametaSoftware/lists/team/members "Team"

[bintray]: https://bintray.com/qameta/maven/allure-java "Bintray"
[bintray-badge]: https://img.shields.io/bintray/v/qameta/maven/allure-java.svg?style=flat

[CONTRIBUTING.md]: .github/CONTRIBUTING.md
[docs]: https://docs.qameta.io/allure/2.0/

# Allure Java Integrations [![bintray-badge][]][bintray]

The repository contains new versions of adaptors for JVM-based test frameworks.

All the artifacts are deployed to `https://dl.bintray.com/qameta/maven`.

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
