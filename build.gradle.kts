import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import org.gradle.jvm.tasks.Jar
import ru.vyarus.gradle.plugin.quality.QualityExtension

buildscript {
    repositories {
        maven("https://plugins.gradle.org/m2/")
        mavenLocal()
        jcenter()
    }

    dependencies {
        classpath("com.jfrog.bintray.gradle:gradle-bintray-plugin:1.8.4")
        classpath("gradle.plugin.com.github.spotbugs:spotbugs-gradle-plugin:1.6.5")
        classpath("io.spring.gradle:dependency-management-plugin:1.0.6.RELEASE")
        classpath("ru.vyarus:gradle-quality-plugin:3.2.0")
    }
}

val linkHomepage by extra("https://qameta.io/allure")
val linkCi by extra("https://ci.qameta.in/job/allure-java_deploy/")
val linkScmUrl by extra("https://github.com/allure-framework/allure-java")
val linkScmConnection by extra("scm:git:git://github.com/allure-framework/allure-java.git")
val linkScmDevConnection by extra("scm:git:ssh://git@github.com:allure-framework/allure-java.git")

val gradleScriptDir by extra("${rootProject.projectDir}/gradle")

tasks.existing(Wrapper::class) {
    gradleVersion = "4.10.2"
    distributionType = Wrapper.DistributionType.ALL
}

plugins {
    java
    id("net.researchgate.release") version "2.7.0"
    id("io.qameta.allure") version "2.5"
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType(JavaCompile::class) {
    options.encoding = "UTF-8"
}

release {
    tagTemplate = "\${version}"
}

val afterReleaseBuild by tasks.existing

allure {
    version = "2.7.0"
    autoconfigure = false
    aspectjweaver = false
}

configure(listOf(rootProject)) {
    description = "Allure Java"
}

configure(subprojects) {
    val project = this
    group = "io.qameta.allure"
    version = version

    apply(plugin = "java")
    apply(plugin = "maven")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "ru.vyarus.quality")
    apply(from = "$gradleScriptDir/bintray.gradle")
    apply(from = "$gradleScriptDir/maven-publish.gradle")

    configure<DependencyManagementExtension> {
        dependencies {
            dependency("com.codeborne:selenide:4.12.2")
            dependency("com.fasterxml.jackson.core:jackson-databind:2.9.7")
            dependency("com.github.tomakehurst:wiremock:2.18.0")
            dependency("com.google.inject:guice:4.2.0")
            dependency("com.google.testing.compile:compile-testing:0.15")
            dependency("com.squareup.okhttp3:okhttp:3.10.0")
            dependency("com.squareup.retrofit2:retrofit:2.4.0")
            dependency("commons-io:commons-io:2.6")
            dependency("io.github.benas:random-beans:3.7.0")
            dependency("io.rest-assured:rest-assured:3.1.0")
            dependency("javax.servlet:javax.servlet-api:4.0.1")
            dependency("javax.ws.rs:javax.ws.rs-api:2.0.1")
            dependency("junit:junit:4.12")
            dependency("net.javacrumbs.json-unit:json-unit:2.0.0.RC1")
            dependency("org.apache.commons:commons-lang3:3.7")
            dependency("org.apache.httpcomponents:httpclient:4.5.6")
            dependency("org.apache.tika:tika-core:1.19.1")
            dependency("org.aspectj:aspectjrt:1.9.1")
            dependency("org.aspectj:aspectjweaver:1.9.1")
            dependency("org.assertj:assertj-core:3.10.0")
            dependency("org.codehaus.groovy:groovy-all:2.5.1")
            dependency("org.freemarker:freemarker:2.3.28")
            dependency("org.hamcrest:hamcrest-library:1.3")
            dependency("org.jbehave:jbehave-core:4.3.4")
            dependency("org.jboss.resteasy:resteasy-client:3.6.1.Final")
            dependency("org.jooq:joor-java-8:0.9.9")
            dependency("org.junit-pioneer:junit-pioneer:0.2.2")
            dependency("org.junit.jupiter:junit-jupiter-api:5.2.0")
            dependency("org.junit.jupiter:junit-jupiter-engine:5.2.0")
            dependency("org.junit.jupiter:junit-jupiter-params:5.2.0")
            dependency("org.junit.platform:junit-platform-launcher:1.2.0")
            dependency("org.mock-server:mockserver-netty:5.4.1")
            dependency("org.mockito:mockito-core:2.19.0")
            dependency("org.slf4j:slf4j-api:1.7.25")
            dependency("org.slf4j:slf4j-simple:1.7.25")
            dependency("org.spockframework:spock-core:1.1-groovy-2.4")
            dependency("org.springframework.boot:spring-boot-autoconfigure:1.5.14.RELEASE")
            dependency("org.springframework:spring-test:4.3.18.RELEASE")
            dependency("org.springframework:spring-webmvc:4.3.18.RELEASE")
            dependency("org.testng:testng:6.14.3")
        }
    }

    tasks.named<Jar>("jar") {
        manifest {
            attributes(mapOf(
                    "Implementation-Title" to project.name,
                    "Implementation-Version" to project.version
            ))
        }
    }

    tasks.named<Test>("test") {
        systemProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug")
        systemProperty("allure.model.indentOutput", "true")
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    configure<QualityExtension> {
        configDir = "$gradleScriptDir/quality-configs"
        pmdVersion = "6.9.0"
    }

    val sourceSets = project.the<SourceSetContainer>()

    val sourceJar by tasks.creating(Jar::class) {
        from(sourceSets.getByName("main").allJava)
        classifier = "sources"
    }

    val javadocJar by tasks.creating(Jar::class) {
        from(tasks.getByName("javadoc"))
        classifier = "javadoc"
    }

    tasks.withType(Javadoc::class) {
        (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
    }

    artifacts.add("archives", sourceJar)
    artifacts.add("archives", javadocJar)

    val bintrayUpload by tasks.existing
    afterReleaseBuild {
        dependsOn(bintrayUpload)
    }

    repositories {
        jcenter()
        mavenLocal()
    }
}
