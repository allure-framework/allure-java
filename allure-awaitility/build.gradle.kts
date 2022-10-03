description = "Allure Awaitlity Integration"

val agent: Configuration by configurations.creating

val awaitilityVersion = "4.2.0"

dependencies {
    agent("org.aspectj:aspectjweaver")
    api(project(":allure-java-commons"))
    implementation("org.awaitility:awaitility:$awaitilityVersion")
    testImplementation("javax.annotation:javax.annotation-api")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation(project(":allure-java-commons-test"))
    testImplementation(project(":allure-junit-platform"))
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