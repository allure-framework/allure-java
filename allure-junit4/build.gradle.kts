description = "Allure JUnit 4 Integration"

val junitVersion = "4.13.2"

dependencies {
    api(project(":allure-java-commons"))
    compileOnly("junit:junit:$junitVersion")
    implementation(project(":allure-test-filter"))
    testAnnotationProcessor(project(":allure-descriptions-javadoc"))
    testImplementation("junit:junit:$junitVersion")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation(project(":allure-java-commons-test"))
    testImplementation(project(":allure-junit-platform"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.jar {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.junit4"
        ))
    }
}

tasks.test {
    useJUnitPlatform()
    exclude("**/samples/*", "SampleTestInDefaultPackage.java")
}
