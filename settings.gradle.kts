rootProject.name = "allure-java"

include("allure-assertj")
include("allure-attachments")
include("allure-citrus")
include("allure-cucumber-jvm")
include("allure-cucumber2-jvm")
include("allure-cucumber3-jvm")
include("allure-cucumber4-jvm")
include("allure-cucumber5-jvm")
include("allure-cucumber6-jvm")
include("allure-descriptions-javadoc")
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
include("allure-model")
include("allure-okhttp")
include("allure-okhttp3")
include("allure-rest-assured")
include("allure-scalatest")
include("allure-selenide")
include("allure-servlet-api")
include("allure-spock")
include("allure-spring-web")
include("allure-test-filter")
include("allure-testng")

plugins {
    id("com.gradle.enterprise") version "3.6.2"
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
