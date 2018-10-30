description = "Allure JUnit 4 Integration"

val agent by configurations.creating

dependencies {
    agent("org.aspectj:aspectjweaver")
    compile("junit:junit")
    compile(project(":allure-java-commons"))
    testCompile("org.assertj:assertj-core")
    testCompile("org.mockito:mockito-core")
    testCompile("org.slf4j:slf4j-simple")
    testCompile(project(":allure-java-commons-test"))
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.junit4"
        ))
    }
}

tasks.named<Test>("test") {
    useJUnit()
    exclude("**/samples/*")
    doFirst {
        jvmArgs("-javaagent:${agent.singleFile}")
    }
}
