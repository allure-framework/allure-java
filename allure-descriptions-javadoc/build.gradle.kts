description = "Allure Javadoc Descriptions"

val agent by configurations.creating

dependencies {
    agent("org.aspectj:aspectjweaver")

    compile(project(":allure-java-commons"))
    compile("commons-io:commons-io")

    testCompile("com.google.testing.compile:compile-testing")
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
                "Automatic-Module-Name" to "io.qameta.allure.description"
        ))
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    doFirst {
        jvmArgs("-javaagent:${agent.singleFile}")
    }
}
