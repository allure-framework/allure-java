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
You can specify custom templateName:
```
.filter(new AllureRestAssured().withTemplate("/templates/custom_template.ftl"))
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


