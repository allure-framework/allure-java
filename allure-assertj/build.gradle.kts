description = "Allure AssertJ Integration"

val agent by configurations.creating

dependencies {
    agent("org.aspectj:aspectjweaver")
    compile("org.assertj:assertj-core")
    compile(project(":allure-java-commons"))
    testCompile("org.junit.jupiter:junit-jupiter-api")
    testCompile("org.slf4j:slf4j-simple")
    testCompile(project(":allure-java-commons-test"))
    testCompile(project(":allure-junit-platform"))
    testRuntime("org.junit.jupiter:junit-jupiter-engine")
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
