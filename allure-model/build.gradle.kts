description = "Allure Model Integration"

val agent by configurations.creating

dependencies {
    agent("org.aspectj:aspectjweaver")

    compile("com.fasterxml.jackson.core:jackson-databind")

    testCompile("io.github.benas:random-beans")
    testCompile("org.assertj:assertj-core")
    testCompile("org.junit-pioneer:junit-pioneer")
    testCompile("org.junit.jupiter:junit-jupiter-api")
    testCompile("org.mockito:mockito-core")
    testCompile("org.slf4j:slf4j-simple")
    testRuntime("org.junit.jupiter:junit-jupiter-engine")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.model"
        ))
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    doFirst {
        jvmArgs("-javaagent:${agent.singleFile}")
    }
}
