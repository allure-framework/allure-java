description = "Allure Citrus Integration"

val agent by configurations.creating

val citrusVersion = "2.8.0"

dependencies {
    agent("org.aspectj:aspectjweaver")
    api(project(":allure-java-commons"))
    compileOnly("com.consol.citrus:citrus-core:$citrusVersion")
    testImplementation("com.consol.citrus:citrus-http:$citrusVersion")
    testImplementation("com.consol.citrus:citrus-java-dsl:$citrusVersion")
    testImplementation("io.github.glytching:junit-extensions")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation(project(":allure-assertj"))
    testImplementation(project(":allure-java-commons-test"))
    testImplementation(project(":allure-junit-platform"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.citrus"
        ))
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    exclude("**/samples/*")
    doFirst {
        jvmArgs("-javaagent:${agent.singleFile}")
    }
}
