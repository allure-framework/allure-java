description = "Allure TestNG 7 Integration"

val testNgVersion = "7.11.0"

dependencies {
    api(project(":allure-java-commons"))
    compileOnly("org.testng:testng:$testNgVersion")
    implementation(project(":allure-test-filter"))
    testAnnotationProcessor("org.slf4j:slf4j-simple")
    testAnnotationProcessor(project(":allure-descriptions-javadoc"))
    testImplementation("com.google.inject:guice")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation("org.testng:testng:$testNgVersion")
    testImplementation(project(":allure-java-commons-test"))
}

tasks.jar {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.testng7"
        ))
    }
    from("src/main/services") {
        into("META-INF/services")
    }
}

tasks.test {
    useTestNG(closureOf<TestNGOptions> {
        suites("src/test/resources/testng.xml")
    })
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
