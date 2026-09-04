import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("jvm")
    id("com.diffplug.spotless")
}

description = "Allure Kotlin Extensions"

dependencies {
    api(project(":allure-java-commons"))
    api("org.jetbrains.kotlin:kotlin-stdlib:2.4.10")

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation(project(":allure-java-commons-test"))
    testImplementation(project(":allure-junit-platform"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    explicitApi()
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        languageVersion.set(KotlinVersion.KOTLIN_2_0)
        apiVersion.set(KotlinVersion.KOTLIN_2_0)
        // The older language level is intentional: it keeps published metadata readable by Kotlin 2.0 consumers.
        freeCompilerArgs.add("-Xsuppress-version-warnings")
        javaParameters.set(true)
    }
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint("1.8.0")
        licenseHeaderFile(
            rootProject.file("gradle/quality-configs/spotless/header.java"),
            "(@file:|package)",
        )
        endWithNewline()
    }
}

tasks.jar {
    manifest {
        attributes(
            mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.kotlin.extensions",
            ),
        )
    }
}

tasks.compileTestKotlin {
    compilerOptions.jvmTarget.set(JvmTarget.fromTarget(tasks.compileTestJava.get().targetCompatibility))
}

tasks.test {
    useJUnitPlatform()
}
