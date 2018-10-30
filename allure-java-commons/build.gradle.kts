description = "Allure Java Commons"

val agent by configurations.creating

dependencies {
    agent("org.aspectj:aspectjweaver")
    compile("org.apache.tika:tika-core")
    compile("org.aspectj:aspectjrt")
    compile("org.jooq:joor-java-8")
    compile("org.slf4j:slf4j-api")
    compile(project(":allure-model"))
    testCompile("io.github.benas:random-beans")
    testCompile("org.assertj:assertj-core")
    testCompile("org.junit-pioneer:junit-pioneer")
    testCompile("org.junit.jupiter:junit-jupiter-api")
    testCompile("org.junit.jupiter:junit-jupiter-params")
    testCompile("org.mockito:mockito-core")
    testCompile("org.slf4j:slf4j-simple")
    testCompile(project(":allure-java-commons-test"))
    testCompile(project(":allure-junit-platform"))
    testRuntime("org.junit.jupiter:junit-jupiter-engine")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.commons"
        ))
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    doFirst {
        jvmArgs("-javaagent:${agent.singleFile}")
    }
}
