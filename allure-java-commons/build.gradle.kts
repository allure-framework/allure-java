plugins {
    id("com.github.johnrengelman.shadow")
}

description = "Allure Java Commons"

dependencies {
    api("org.slf4j:slf4j-api")
    api(project(":allure-model"))
    compileOnly("org.aspectj:aspectjrt")
    internal("com.fasterxml.jackson.core:jackson-databind")
    testImplementation("io.github.benas:random-beans")
    testImplementation("io.github.glytching:junit-extensions")
    testImplementation("org.apache.commons:commons-lang3")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation(project(":allure-java-commons-test"))
    testImplementation(project(":allure-junit-platform"))
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
            include(dependency("com.fasterxml.jackson.core::"))
        }
        exclude("**/module-info.class")
        exclude("META-INF/LICENSE*.md")
        mergeServiceFiles()
        manifest {
            attributes(mapOf(
                    "Specification-Title" to project.name,
                    "Implementation-Title" to project.name,
                    "Implementation-Version" to project.version,
                    "Automatic-Module-Name" to "io.qameta.allure.commons"
            ))
        }
    }

    assemble {
        dependsOn(shadowJar)
    }

    test {
        dependsOn(shadowJar)
        useJUnitPlatform()
    }
}
