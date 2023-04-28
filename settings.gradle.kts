rootProject.name = "allure-java"

include("allure-assertj")
include("allure-attachments")
include("allure-awaitility")
include("allure-bom")
include("allure-citrus")
include("allure-cucumber2-jvm")
include("allure-cucumber3-jvm")
include("allure-cucumber4-jvm")
include("allure-cucumber5-jvm")
include("allure-cucumber6-jvm")
include("allure-cucumber7-jvm")
include("allure-descriptions-javadoc")
include("allure-grpc")
include("allure-hamcrest")
include("allure-httpclient")
include("allure-java-commons")
include("allure-java-commons-test")
include("allure-java-migration")
include("allure-jax-rs")
include("allure-jbehave")
include("allure-jsonunit")
include("allure-junit-platform")
include("allure-junit4")
include("allure-junit4-aspect")
include("allure-junit5")
include("allure-junit5-assert")
include("allure-karate")
include("allure-model")
include("allure-okhttp")
include("allure-okhttp3")
include("allure-reader")
include("allure-rest-assured")
include("allure-scalatest")
include("allure-selenide")
include("allure-servlet-api")
include("allure-spock")
include("allure-spock2")
include("allure-spring-web")
include("allure-test-filter")
include("allure-testng")

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
    plugins {
        id("com.diffplug.spotless") version "6.11.0"
        id("com.github.johnrengelman.shadow") version "7.1.2"
        id("com.gradle.enterprise") version "3.12.5"
        id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
        id("io.qameta.allure-adapter") version "2.11.2"
        id("io.qameta.allure-aggregate-report") version "2.11.2"
        id("io.qameta.allure-download") version "2.11.2"
        id("io.qameta.allure-report") version "2.11.2"
        id("io.spring.dependency-management") version "1.1.0"
        id("ru.vyarus.quality") version "4.7.0"
        id("com.google.protobuf") version "0.9.1"
        kotlin("jvm") version "1.7.10"
    }
}

plugins {
    id("com.gradle.enterprise")
}

val isCiServer = System.getenv().containsKey("CI")

if (isCiServer) {
    gradleEnterprise {
        buildScan {
            termsOfServiceUrl = "https://gradle.com/terms-of-service"
            termsOfServiceAgree = "yes"
            tag("CI")
        }
    }
}
