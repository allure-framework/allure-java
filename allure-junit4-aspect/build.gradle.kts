description = "Allure JUnit 4 AspectJ integration for Gradle test execution"

val junitVersion = "4.13.2"

dependencies {
    api(project(":allure-junit4"))
    compileOnly("junit:junit:$junitVersion")
    compileOnly("org.aspectj:aspectjrt")
    testImplementation("junit:junit:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.aspectj:aspectjrt")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation(project(":allure-junit-platform"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.jar {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.junit4aspect"
        ))
    }
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            description.set(project.description)
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
