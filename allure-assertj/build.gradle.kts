description = "Allure AssertJ Integration"

// Compile the optional type used by the regression fixture without adding it to the test runtime.
val missingDependency by sourceSets.creating

dependencies {
    api(project(":allure-java-commons"))
    compileOnly("org.aspectj:aspectjrt")
    compileOnly("org.assertj:assertj-core")
    testImplementation("org.aspectj:aspectjweaver")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.slf4j:slf4j-simple")
    testCompileOnly(missingDependency.output)
    testImplementation(project(":allure-java-commons-test"))
    testImplementation(project(":allure-junit-platform"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.jar {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.assertj"
        ))
    }
}

tasks.test {
    useJUnitPlatform()
}
