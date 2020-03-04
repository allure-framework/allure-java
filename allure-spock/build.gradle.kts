description = "Allure Spock Framework Integration"

apply(plugin = "groovy")

val agent: Configuration by configurations.creating

val spockFrameworkVersion = "1.2-groovy-2.5"
val apacheCommonsCollection4 = "4.4"
val apacheCommonsLang3 = "3.9"

dependencies {
    agent("org.aspectj:aspectjweaver")
    api(project(":allure-java-commons"))
    implementation("org.spockframework:spock-core:$spockFrameworkVersion")
    implementation("org.apache.commons:commons-collections4:$apacheCommonsCollection4")
    implementation("org.apache.commons:commons-lang3:$apacheCommonsLang3")
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
    doFirst {
        jvmArgs("-javaagent:${agent.singleFile}")
    }
}
