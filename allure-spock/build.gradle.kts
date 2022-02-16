description = "Allure Spock Framework Integration"

apply(plugin = "groovy")

val spockFrameworkVersion = "2.1-groovy-3.0"

dependencies {
    api(project(":allure-java-commons"))
    implementation("org.spockframework:spock-core:$spockFrameworkVersion")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.codehaus.groovy:groovy-all")
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
                "Automatic-Module-Name" to "io.qameta.allure.spock"
        ))
    }
}

tasks.test {
    useJUnitPlatform()
    exclude("**/samples/*")
}
