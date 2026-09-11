plugins {
    id("com.gradleup.shadow")
}

description = "Allure Java Commons"

shadow {
    // Only the final JAR, with our descriptor added, belongs in the publication.
    addShadowVariantIntoJavaComponent.set(false)
}

dependencies {
    api("org.slf4j:slf4j-api")
    api(project(":allure-model"))
    compileOnly("org.aspectj:aspectjrt")
    internal("com.fasterxml.jackson.core:jackson-databind")
    testImplementation("io.github.benas:random-beans")
    testImplementation("io.github.glytching:junit-extensions")
    testImplementation("org.apache.commons:commons-lang3")
    testImplementation("org.assertj:assertj-core")
    testImplementation(project(":allure-assertj"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation(project(":allure-java-commons-test"))
    testImplementation(project(":allure-junit-platform"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Compile the descriptor separately: Jackson is relocated into this module in the published JAR.
val compileModuleInfo = tasks.register<JavaCompile>("compileModuleInfo") {
    source("src/main/module/module-info.java")
    classpath = configurations.compileClasspath.get()
    destinationDirectory.set(layout.buildDirectory.dir("classes/module-info"))
    options.release.set(17)
    modularity.inferModulePath.set(false)
    inputs.files(sourceSets.main.get().output).withPropertyName("patchedClasses")
    options.compilerArgumentProviders.add(org.gradle.process.CommandLineArgumentProvider {
        listOf("--module-path", classpath.asPath)
    })
    dependsOn(tasks.classes)
    options.compilerArgs.addAll(listOf(
        "--patch-module", "io.qameta.allure.commons=${sourceSets.main.get().output.asPath}"
    ))
}

val modularJar = tasks.register<Jar>("modularJar") {
    archiveClassifier.set("")
    from(tasks.shadowJar.map { zipTree(it.archiveFile) }) {
        exclude("META-INF/MANIFEST.MF")
    }
    from(compileModuleInfo)
    manifest.from(tasks.shadowJar.get().manifest)
}

configurations.forEach { configuration ->
    configuration.outgoing.apply {
        val removed = artifacts.removeAll { it.classifier.isNullOrEmpty() }
        if (removed) {
            artifact(modularJar) {
                classifier = ""
            }
        }
    }
}

tasks {
    jar {
        dependsOn(modularJar)
        enabled = false
    }

    shadowJar {
        archiveClassifier.set("shadow")
        configurations = project.configurations.runtimeClasspath.map { listOf(it) }
        relocate("com.fasterxml.jackson", "io.qameta.allure.internal.shadowed.jackson")
        dependencies {
            include(dependency("com.fasterxml.jackson.core:.*:.*"))
        }
        exclude("**/module-info.class")
        exclude("META-INF/LICENSE*.md")
        mergeServiceFiles()
        manifest {
            attributes(mapOf(
                    "Specification-Title" to project.name,
                    "Implementation-Title" to project.name,
                    "Implementation-Version" to project.version
            ))
        }
    }

    assemble {
        dependsOn(modularJar)
    }

    test {
        dependsOn(modularJar)
        useJUnitPlatform()
    }

    sourcesJar {
        from("src/main/module")
    }
}
