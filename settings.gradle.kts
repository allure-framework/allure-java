rootProject.name = "allure-java"

include("allure-assertj")
include("allure-awaitility")
include("allure-bom")
include("allure-citrus")
include("allure-cucumber7-jvm")
include("allure-descriptions-javadoc")
include("allure-grpc")
include("allure-hamcrest")
include("allure-httpclient")
include("allure-httpclient5")
include("allure-java-commons")
include("allure-java-commons-test")
include("allure-jax-rs")
include("allure-jbehave5")
include("allure-jooq")
include("allure-jsonunit")
include("allure-junit-platform")
include("allure-junit4")
include("allure-junit4-aspect")
include("allure-jupiter")
include("allure-jupiter-assert")
include("allure-karate")
include("allure-model")
include("allure-okhttp3")
include("allure-playwright")
include("allure-rest-assured")
include("allure-scalatest")
include("allure-selenium-bidi")
include("allure-selenide")
include("allure-servlet-api")
include("allure-spock2")
include("allure-spring-web")
include("allure-testng")

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
    plugins {
        id("com.diffplug.spotless") version "8.6.0"
        id("com.gradleup.shadow") version "9.4.2"
        id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
        id("io.qameta.allure") version "4.1.0"
        id("io.spring.dependency-management") version "1.1.7"
        id("com.google.protobuf") version "0.10.0"
        id("com.github.spotbugs") version "6.5.6"
        kotlin("jvm") version "2.4.0"
    }
}

plugins {
    id("com.gradle.develocity") version "4.4.2" apply false
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
