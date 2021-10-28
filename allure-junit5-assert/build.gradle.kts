description = "Allure Junit5 Assertions Integration"

dependencies {
    api(project(":allure-junit5"))
    compileOnly("org.aspectj:aspectjrt")
    implementation("org.junit.jupiter:junit-jupiter-api")
    testAnnotationProcessor(project(":allure-descriptions-javadoc"))
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation(project(":allure-java-commons-test"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.jar {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.junit5-assert"
        ))
    }
}

tasks.test {
    useJUnitPlatform()
}

