import org.gradle.jvm.tasks.Jar

// inspired on
// https://github.com/Jolanrensen/gradle-crossbuild-sample
// https://github.com/gabrieljones/crossbuild-hello
// https://github.com/newrelic/newrelic-java-agent/blob/scala3-cross-build/newrelic-scala-api/build.gradle.kts
description = "Allure ScalaTest Integration"

plugins {
    scala
    id("com.github.prokod.gradle-crossbuild") version "0.16.0"
}

val scala212 = "2.12"
val scala213 = "2.13"

project.base.archivesName.set("allure-scalatest")

crossBuild {
    scalaVersionsCatalog = mapOf(
        scala212 to "2.12.19",
        scala213 to "2.13.14"
    )
    builds {
        register("scala") {
            scalaVersions = setOf(scala212, scala213)
        }
    }
}

val crossBuildScala_212Jar by tasks.getting
val crossBuildScala_213Jar by tasks.getting

publishing {
    publications {
        register("crossBuildScala_212", MavenPublication::class) {
            artifact(crossBuildScala_212Jar)
        }
        register("crossBuildScala_213", MavenPublication::class) {
            artifact(crossBuildScala_213Jar)
        }
    }
}

dependencies {
    api(project(":allure-java-commons"))
    implementation("org.scalatest:scalatest_$scala213:3.2.19")
    implementation("org.scala-lang.modules:scala-collection-compat_$scala213:2.12.0")
    testAnnotationProcessor(project(":allure-descriptions-javadoc"))
    testImplementation("io.github.glytching:junit-extensions")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation(project(":allure-assertj"))
    testImplementation(project(":allure-java-commons-test"))
    testImplementation(project(":allure-junit-platform"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

val scaladocJar by tasks.creating(Jar::class) {
    from(tasks.getByName("scaladoc"))
    archiveClassifier.set("scaladoc")
}

artifacts.add("archives", scaladocJar)

tasks.jar {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.scalatest"
        ))
    }
}

tasks.test {
    systemProperty("junit.jupiter.execution.parallel.enabled", "false")
    useJUnitPlatform()
    exclude("**/testdata/*")
}
