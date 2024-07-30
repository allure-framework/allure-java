import org.gradle.jvm.tasks.Jar

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

val scaladocJar by tasks.creating(Jar::class) {
    from(tasks.getByName("scaladoc"))
    archiveClassifier.set("scaladoc")
}

tasks.withType<PublishToMavenLocal>().configureEach {
    val predicate = provider {
        publication != publishing.publications["maven"]
    }
    onlyIf("disable default maven publication") {
        predicate.get()
    }
}

tasks.withType<PublishToMavenRepository>().configureEach {
    val predicate = provider {
        publication != publishing.publications["maven"]
    }
    onlyIf("disable default maven publication") {
        predicate.get()
    }
}

publishing {
    publications {
        create<MavenPublication>("crossBuildScala_212") {
            from(components["crossBuildScala_212"])
            artifact(scaladocJar)
            artifact(tasks.sourcesJar)
        }
        create<MavenPublication>("crossBuildScala_213") {
            from(components["crossBuildScala_213"])
            artifact(scaladocJar)
            artifact(tasks.sourcesJar)
        }
    }
}

signing {
    sign(publishing.publications["crossBuildScala_212"])
    sign(publishing.publications["crossBuildScala_213"])
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
