description = "Allure JsonUnit Integration"

val agent by configurations.creating

val jsonUnitVersion = "2.0.0.RC1"

dependencies {
    agent("org.aspectj:aspectjweaver")

    compile("org.apache.commons:commons-lang3")
    compile("net.javacrumbs.json-unit:json-unit:$jsonUnitVersion")
    compile(project(":allure-attachments"))
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
                "Automatic-Module-Name" to "io.qameta.allure.jsonunit"
        ))
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    doFirst {
        jvmArgs("-javaagent:${agent.singleFile}")
    }
}

