description = "Allure Awaitlity Integration"

val awaitilityVersion = "4.2.0"

dependencies {
    api(project(":allure-java-commons"))
    implementation("org.awaitility:awaitility:$awaitilityVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation(project(":allure-java-commons-test"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.jar {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.awaitility"
        ))
    }
}

tasks.test {
    useJUnitPlatform()
}