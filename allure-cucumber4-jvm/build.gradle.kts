description = "Allure CucumberJVM 4.0"

val agent by configurations.creating

val cucumberVersion = "4.2.1"

dependencies {
    agent("org.aspectj:aspectjweaver")
    compile(project(":allure-java-commons"))

    compile("io.cucumber:cucumber-core:$cucumberVersion")
    compile("io.cucumber:cucumber-java:$cucumberVersion")

    testCompile("io.cucumber:cucumber-testng:$cucumberVersion")
    testCompile(project(":allure-java-commons-test"))
    testCompile("commons-io:commons-io")
    testCompile("org.assertj:assertj-core")

    testCompile("org.junit.jupiter:junit-jupiter-api")
    testRuntime("org.junit.jupiter:junit-jupiter-engine")
    testCompile("io.github.glytching:junit-extensions")

    testCompile("org.slf4j:slf4j-simple")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.cucumber4jvm"
        ))
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    doFirst {
        jvmArgs("-javaagent:${agent.singleFile}")
    }
}
