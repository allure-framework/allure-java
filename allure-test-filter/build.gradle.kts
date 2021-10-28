plugins {
    id("com.github.johnrengelman.shadow")
}

description = "Allure Test Filter"

dependencies {
    implementation(project(":allure-java-commons"))
    internal("com.fasterxml.jackson.core:jackson-databind")
    testAnnotationProcessor("org.slf4j:slf4j-simple")
    testAnnotationProcessor(project(":allure-descriptions-javadoc"))
    testImplementation("io.github.glytching:junit-extensions")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation(project(":allure-assertj"))
    testImplementation(project(":allure-java-commons-test"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

configurations.forEach { configuration ->
    configuration.outgoing.apply {
        val removed = artifacts.removeAll { it.classifier.isNullOrEmpty() }
        if (removed) {
            artifact(tasks.shadowJar) {
                classifier = ""
            }
        }
    }
}

tasks {
    jar {
        dependsOn(shadowJar)
        enabled = false
    }

    shadowJar {
        archiveClassifier.set("")
        relocate("com.fasterxml.jackson", "io.qameta.allure.internal.shadowed.jackson")
        dependencies {
            // we only need relocate to use jackson, shadowed in allure-java-commons
            include(dependency("x:x:x"))
        }
        mergeServiceFiles()
        manifest {
            attributes(mapOf(
                    "Specification-Title" to project.name,
                    "Implementation-Title" to project.name,
                    "Implementation-Version" to project.version,
                    "Automatic-Module-Name" to "io.qameta.allure.testfilter"
            ))
        }
        from("src/main/services") {
            into("META-INF/services")
        }
    }

    assemble {
        dependsOn(shadowJar)
    }

    test {
        dependsOn(shadowJar)
        systemProperty("junit.jupiter.execution.parallel.enabled", "false")
        useJUnitPlatform()
        exclude("**/features/*")
    }
}

