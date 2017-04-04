# Allure Java Integrations

The repository contains new versions of adaptors for JVM-based test frameworks.

All the artifacts are deployed to `https://dl.bintray.com/qameta/maven`.

There are few features that not available yet (compare to Allure1):

* Name templates for `@Steps` and `@Attachments` (I mean `{index}` and `{method}` placeholders). We are going to rework this functionallity, proably add an ability to change template engine.
* Descriptions fuctionallity is compitly missed up. 

## TestNG 

The new TestNG adaptors is pretty much ready. To use the adaptor you should add the following dependency:

```xml
<dependency>
   <groupId>io.qameta.allure</groupId>
   <artifactId>allure-testng</artifactId>
   <version>2.0-BETA1</version>
</dependency>
```

also you need to configure AspectJ weaver to support steps.

## JUnit 4

The first draft of a new JUnit 4 adaptor is ready. To use the adaptor you should add the following dependency:

```xml
<dependency>
   <groupId>io.qameta.allure</groupId>
   <artifactId>allure-junit4</artifactId>
   <version>2.0-BETA1</version>
</dependency>
```

## JUnit 5

To use JUnit 5 simply add the following dependency to your project:

```xml
<dependency>
   <groupId>io.qameta.allure</groupId>
   <artifactId>allure-junit5</artifactId>
   <version>2.0-BETA4</version>
</dependency>
```
