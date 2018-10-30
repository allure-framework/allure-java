description = "Allure AssertJ Integration"

val agent by configurations.creating

dependencies {
    agent("org.aspectj:aspectjweaver")

    compile(project(":allure-java-commons"))
    compile("org.assertj:assertj-core")

    testCompile("org.slf4j:slf4j-simple")
    testCompile("org.junit.jupiter:junit-jupiter-api")
    testRuntime("org.junit.jupiter:junit-jupiter-engine")
    testCompile(project(":allure-junit-platform"))
    testCompile(project(":allure-java-commons-test"))
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.assertj"
        ))
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    doFirst {
        jvmArgs("-javaagent:${agent.singleFile}")
    }
}
