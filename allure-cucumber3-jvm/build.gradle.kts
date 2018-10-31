description = "Allure CucumberJVM 3.0 Integration"

val agent by configurations.creating

val cucumberVersion = "3.0.0"

dependencies {
    agent("org.aspectj:aspectjweaver")
    compile("io.cucumber:cucumber-core:$cucumberVersion")
    compile("io.cucumber:cucumber-java:$cucumberVersion")
    compile(project(":allure-java-commons"))

    testCompile("commons-io:commons-io")
    testCompile("io.cucumber:cucumber-testng:$cucumberVersion")
    testCompile("io.github.glytching:junit-extensions")
    testCompile("org.assertj:assertj-core")
    testCompile("org.junit.jupiter:junit-jupiter-api")
    testCompile("org.slf4j:slf4j-simple")
    testCompile(project(":allure-java-commons-test"))
    testRuntime("org.junit.jupiter:junit-jupiter-engine")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.cucumber3jvm"
        ))
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    doFirst {
        jvmArgs("-javaagent:${agent.singleFile}")
    }
}
