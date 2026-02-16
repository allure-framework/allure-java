rootProject.name = "allure-java"

include("allure-assertj")
include("allure-attachments")
include("allure-awaitility")
include("allure-bom")
include("allure-citrus")
include("allure-cucumber4-jvm")
include("allure-cucumber5-jvm")
include("allure-cucumber6-jvm")
include("allure-cucumber7-jvm")
include("allure-descriptions-javadoc")
include("allure-grpc")
include("allure-hamcrest")
include("allure-httpclient")
include("allure-httpclient5")
include("allure-java-commons")
include("allure-java-commons-test")
include("allure-jax-rs")
include("allure-jbehave")
include("allure-jbehave5")
include("allure-jooq")
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
        id("com.diffplug.spotless") version "6.25.0"
        id("io.github.goooler.shadow") version "8.1.8"
        id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
        id("io.qameta.allure-adapter") version "3.0.1"
        id("io.qameta.allure-aggregate-report") version "3.0.1"
        id("io.qameta.allure-download") version "3.0.1"
        id("io.qameta.allure-report") version "3.0.1"
        id("io.spring.dependency-management") version "1.1.7"
        id("com.google.protobuf") version "0.9.6"
        id("com.github.spotbugs") version "6.4.7"
        kotlin("jvm") version "2.0.0"
    }
}

plugins {
    id("com.gradle.develocity") version "3.17.5" apply false
}

val isCiServer = System.getenv().containsKey("CI")

if (isCiServer) {
    apply(plugin = "com.gradle.develocity")
    develocity {
        buildScan {
            termsOfUseUrl = "https://gradle.com/terms-of-service"
            termsOfUseAgree = "yes"
            tag("CI")
        }
    }
}
