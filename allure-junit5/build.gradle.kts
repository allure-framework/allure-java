description = "Allure JUnit 5 Integration"

dependencies {
    api(project(":allure-junit-platform"))
    compileOnly("org.junit.jupiter:junit-jupiter-api")
    compileOnly("org.junit.platform:junit-platform-launcher")
    testAnnotationProcessor(project(":allure-descriptions-javadoc"))
    testImplementation("io.github.glytching:junit-extensions")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.junit.platform:junit-platform-launcher")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation(project(":allure-assertj"))
    testImplementation(project(":allure-java-commons-test"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.jar {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.junit5"
        ))
    }
    from("src/main/services") {
        into("META-INF/services")
    }
}

tasks.test {
    systemProperty("junit.jupiter.execution.parallel.enabled", "false")
    useJUnitPlatform()
    exclude("**/features/*")
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

