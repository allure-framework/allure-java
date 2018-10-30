description = "Allure CucumberJVM 3.0 Integration"

val agent by configurations.creating

val cucumberVersion = "3.0.0"

dependencies {
    agent("org.aspectj:aspectjweaver")
    compile("io.cucumber:cucumber-core:$cucumberVersion")
    compile("io.cucumber:cucumber-java:$cucumberVersion")
    compile(project(":allure-java-commons"))
    testCompile("io.cucumber:cucumber-testng:$cucumberVersion")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.cucumber3jvm"
        ))
    }
}

tasks.named<Test>("test") {
    doFirst {
        jvmArgs("-javaagent:${agent.singleFile}")
    }
}
