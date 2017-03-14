# Allure Java Integrations

The repository contains new versions of adaptors for JVM-based test frameworks.

All the artifacts are deployed to `https://dl.bintray.com/qameta/maven`.

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

Is not ready yet. We are waiting for `5.0 M4` (SPI support for listeners) and https://github.com/junit-team/junit5/issues/618
