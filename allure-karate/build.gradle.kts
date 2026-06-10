description = "Allure Karate Integration"

val karateVersion = "2.0.10"

configurations {
    testImplementation {
        exclude(group="ch.qos.logback", module = "logback-classic")
    }
}

dependencies {
    api(project(":allure-java-commons"))
    compileOnly("io.karatelabs:karate-core:${karateVersion}")
    testAnnotationProcessor("org.slf4j:slf4j-simple")
    testImplementation("io.karatelabs:karate-core:${karateVersion}")
    testImplementation("io.github.glytching:junit-extensions")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation(project(":allure-java-commons-test"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.jar {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.karate"
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

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
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
