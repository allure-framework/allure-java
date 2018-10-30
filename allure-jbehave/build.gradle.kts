description = "Allure JBehave Integration"

val agent by configurations.creating

dependencies {
    agent("org.aspectj:aspectjweaver")

    compile(project(":allure-java-commons"))
    compile("org.jbehave:jbehave-core")

    testCompile("org.slf4j:slf4j-simple")
    testCompile("org.mockito:mockito-core")
    testCompile("org.assertj:assertj-core")
    testCompile(project(":allure-java-commons-test"))
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.jbehave"
        ))
    }
}

tasks.named<Test>("test") {
    useJUnit()
    doFirst {
        jvmArgs("-javaagent:${agent.singleFile}")
    }
}

