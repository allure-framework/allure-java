description = "Allure JUnit 4 Integration"

val agent by configurations.creating

val junitVersion = "4.12"

dependencies {
    agent("org.aspectj:aspectjweaver")
    compile("junit:junit:$junitVersion")
    compile(project(":allure-java-commons"))
    testCompile("org.assertj:assertj-core")
    testCompile("org.junit-pioneer:junit-pioneer")
    testCompile("org.junit.jupiter:junit-jupiter-api")
    testCompile("org.mockito:mockito-core")
    testCompile("org.slf4j:slf4j-simple")
    testCompile(project(":allure-java-commons-test"))
    testCompile(project(":allure-junit-platform"))
    testRuntime("org.junit.jupiter:junit-jupiter-engine")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.junit4"
        ))
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    exclude("**/samples/*", "SampleTestInDefaultPackage.java")
    doFirst {
        jvmArgs("-javaagent:${agent.singleFile}")
    }
}
