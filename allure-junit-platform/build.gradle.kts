description = "Allure JUnit Platform Integration"

dependencies {
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
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.junitplatform"
        ))
    }
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

val spiOffJar: Jar by tasks.creating(Jar::class) {
    from(sourceSets.getByName("main").output)
    archiveClassifier.set("spi-off")
}

publishing {
    publications {
        named<MavenPublication>("maven") {
            artifact(spiOffJar)
        }
    }
}
