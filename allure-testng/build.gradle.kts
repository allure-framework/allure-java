description = "Allure TestNG 7 Integration"

val testNgVersion = "7.12.0"

dependencies {
    api(project(":allure-java-commons"))
    compileOnly("org.testng:testng:$testNgVersion")
    testAnnotationProcessor("org.slf4j:slf4j-simple")
    testImplementation("com.google.inject:guice")
    testImplementation("org.assertj:assertj-core")
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
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.testng"
        ))
    }
    from("src/main/services") {
        into("META-INF/services")
    }
}

tasks.test {
    useJUnitPlatform()
    exclude("**/samples/*")
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
