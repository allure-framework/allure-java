description = "Allure Spock 2 Framework Integration"

plugins {
    groovy
}

val spockFrameworkVersion = "2.4-groovy-5.0"
val groovyVersion = "5.1.2"

dependencies {
    api(project(":allure-java-commons"))
    compileOnly("org.aspectj:aspectjrt")
    compileOnly("org.spockframework:spock-core:$spockFrameworkVersion")
    testAnnotationProcessor("org.slf4j:slf4j-simple")
    testImplementation("io.github.glytching:junit-extensions")
    testImplementation("org.apache.commons:commons-lang3")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.apache.groovy:groovy:${groovyVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation("org.spockframework:spock-core:$spockFrameworkVersion")
    testImplementation(project(":allure-assertj"))
    testImplementation(project(":allure-java-commons-test"))
    testImplementation(project(":allure-junit-platform"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.jar {
    from("src/main/services") {
        into("META-INF/services")
    }
}

tasks.test {
    maxHeapSize = "1024m"
    useJUnitPlatform()
    exclude("**/samples/*")
}

val compileSpiOffModuleInfo = tasks.register<JavaCompile>("compileSpiOffModuleInfo") {
    source("src/spi-off/java/module-info.java")
    classpath = configurations.compileClasspath.get()
    destinationDirectory.set(layout.buildDirectory.dir("classes/spi-off-module-info"))
    options.release.set(17)
    modularity.inferModulePath.set(false)
    inputs.files(sourceSets.main.get().output).withPropertyName("patchedClasses")
    options.compilerArgumentProviders.add(CommandLineArgumentProvider {
        listOf("--module-path", classpath.asPath)
    })
    dependsOn(tasks.jar)
    options.compilerArgs.addAll(listOf(
        "--patch-module", "io.qameta.allure.spock2=${sourceSets.main.get().output.asPath}"
    ))
}

val spiOffJar = tasks.register<Jar>("spiOffJar") {
    from(sourceSets.main.get().output) {
        exclude("module-info.class")
    }
    from(compileSpiOffModuleInfo)
    manifest.from(tasks.jar.get().manifest)
    archiveClassifier.set("spi-off")
}

publishing {
    publications {
        named<MavenPublication>("maven") {
            artifact(spiOffJar)
        }
    }
}
