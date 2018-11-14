description = "Allure CucumberJVM Integration"

val agent by configurations.creating

val cucumberVersion = "1.2.5"

dependencies {
    agent("org.aspectj:aspectjweaver")
    compile("info.cukes:cucumber-core:$cucumberVersion")
    compile("info.cukes:cucumber-java:$cucumberVersion")
    compile("info.cukes:gherkin:2.12.2")
    compile(project(":allure-java-commons"))

    testCompile("info.cukes:cucumber-testng:$cucumberVersion")
    testCompile("commons-io:commons-io")
    testCompile("io.github.glytching:junit-extensions")
    testCompile("org.assertj:assertj-core")
    testCompile("org.junit.jupiter:junit-jupiter-api")
    testCompile("org.slf4j:slf4j-simple")
    testCompile(project(":allure-java-commons-test"))
    testCompile(project(":allure-junit-platform"))
    testRuntime("org.junit.jupiter:junit-jupiter-engine")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.cucumberjvm"
        ))
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    doFirst {
        jvmArgs("-javaagent:${agent.singleFile}")
    }
}
