description = "Allure CucumberJVM 2.0 Integration"

val agent by configurations.creating

val cucumberVersion = "2.3.1"

dependencies {
    agent("org.aspectj:aspectjweaver")

    compile(project(":allure-java-commons"))
    compile("io.cucumber:cucumber-core:$cucumberVersion")
    compile("io.cucumber:cucumber-java:$cucumberVersion")

    testCompile("commons-io:commons-io")
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
                "Automatic-Module-Name" to "io.qameta.allure.cucumber2jvm"
        ))
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    doFirst {
        jvmArgs("-javaagent:${agent.singleFile}")
    }
}
