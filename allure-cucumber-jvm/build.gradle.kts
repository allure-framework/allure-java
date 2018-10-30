description = "Allure CucumberJVM Integration"

val agent by configurations.creating

val cucumberVersion = "1.2.5"

dependencies {
    agent("org.aspectj:aspectjweaver")
    compile("info.cukes:cucumber-core:$cucumberVersion")
    compile("info.cukes:cucumber-java:$cucumberVersion")
    compile("info.cukes:cucumber-junit:$cucumberVersion")
    compile("info.cukes:gherkin:2.12.2")
    compile(project(":allure-java-commons"))
    testCompile("junit:junit:4.12")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.cucumberjvm"
        ))
    }
}

tasks.named<Test>("test") {
    doFirst {
        jvmArgs("-javaagent:${agent.singleFile}")
    }
}
