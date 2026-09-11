description = "Allure JUnit Platform Integration"

dependencies {
    implementation("org.slf4j:slf4j-api")
    api(project(":allure-java-commons"))
    implementation("org.junit.jupiter:junit-jupiter-api")
    implementation("org.junit.platform:junit-platform-launcher")
    testAnnotationProcessor("org.slf4j:slf4j-simple")
    testImplementation("io.github.glytching:junit-extensions")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation(project(":allure-assertj"))
    testImplementation(project(":allure-java-commons-test"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.jar {
    from("src/main/services") {
        into("META-INF/services")
    }
}

tasks.test {
    // The Allure Gradle adapter adds this module's published artifact to the
    // test runtime classpath, so make the jar/task relationship explicit when
    // jar and test are scheduled in the same build.
    dependsOn(tasks.jar)
    systemProperty("junit.jupiter.execution.parallel.enabled", "false")
    useJUnitPlatform()
    exclude("**/features/*")
}

tasks.named<Pmd>("pmdMain") {
    // PMD type resolution reads the main compile classpath, which also
    // contains this module's published artifact via the Allure adapter setup.
    dependsOn(tasks.jar)
}

tasks.javadoc {
    dependsOn(tasks.jar)
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
        "--patch-module", "io.qameta.allure.junitplatform=${sourceSets.main.get().output.asPath}"
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
