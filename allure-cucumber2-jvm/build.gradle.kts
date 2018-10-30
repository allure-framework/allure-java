description = "Allure CucumberJVM 2.0 Integration"

val agent by configurations.creating

val cucumberVersion = "2.3.1"

dependencies {
    agent("org.aspectj:aspectjweaver")

    compile(project(":allure-java-commons"))
    compile("io.cucumber:cucumber-core:$cucumberVersion")
    compile("io.cucumber:cucumber-java:$cucumberVersion")

    testCompile("io.cucumber:cucumber-testng:$cucumberVersion")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.cucumber2jvm"
        ))
    }
}

tasks.named<Test>("test") {
    doFirst {
        jvmArgs("-javaagent:${agent.singleFile}")
    }
}
