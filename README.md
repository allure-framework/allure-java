# Allure Java Integrations

The repository contains new versions of adaptors for JVM-based test frameworks.

## TestNG 

The new TestNG adaptors is pretty much ready! It is not available in Maven Central yet, but you can build and check it out.
Run 

```bash
$ ./gradlew clean build install
```

to build the project and install it to Maven local repository. Then you can use the following dependency

```xml
<dependency>
   <groupId>io.qameta.allure</groupId>
   <artifactId>allure-testng</artifactId>
   <version>1.0-SNAPSHOT</version>
</dependency>
```

also you need to configure AspectJ weaver to support steps.

## JUnit 4

Is not ready yet

## JUnit 5

Is not ready yet. We are waiting for `5.0 M4` (SPI support for listeners) and https://github.com/junit-team/junit5/issues/618
