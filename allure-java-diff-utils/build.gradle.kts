description = "Allure java-diff-utils integration"

val javaDiffUtils = "4.15"

dependencies {
    api(project(":allure-attachments"))
    implementation("io.github.java-diff-utils:java-diff-utils:$javaDiffUtils")
    testImplementation("javax.annotation:javax.annotation-api")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation(project(":allure-java-commons-test"))
    testImplementation(project(":allure-junit-platform"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.jar {
    manifest {
        attributes(mapOf(
            "Automatic-Module-Name" to "io.qameta.allure.diff"
        ))
    }
}

tasks.test {
    useJUnitPlatform()
}