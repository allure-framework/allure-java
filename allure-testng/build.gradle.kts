description = "Allure TestNG 7 Integration"

val testNgVersion = "7.12.0"

dependencies {
    implementation("org.slf4j:slf4j-api")
    api(project(":allure-java-commons"))
    compileOnly("org.testng:testng:$testNgVersion")
    testAnnotationProcessor("org.slf4j:slf4j-simple")
    testImplementation("com.google.inject:guice")
    testImplementation("org.assertj:assertj-core")
    testImplementation(project(":allure-assertj"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation(project(":allure-jupiter"))
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation("org.testng:testng:$testNgVersion")
    testImplementation(project(":allure-java-commons-test"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.jar {
    from("src/main/services") {
        into("META-INF/services")
    }
}

tasks.test {
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
    options.compilerArgumentProviders.add(org.gradle.process.CommandLineArgumentProvider {
        listOf("--module-path", classpath.asPath)
    })
    dependsOn(tasks.jar)
    options.compilerArgs.addAll(listOf(
        "--patch-module", "io.qameta.allure.testng=${sourceSets.main.get().output.asPath}"
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
