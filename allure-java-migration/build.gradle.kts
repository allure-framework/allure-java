description = "Allure Java Migration Utils"

val agent by configurations.creating

val junitVersion = "4.12"
val testNgVersion = "6.14.3"

dependencies {
    agent("org.aspectj:aspectjweaver")

    compile("org.apache.commons:commons-lang3")
    compile("org.aspectj:aspectjrt")
    compile(project(":allure-java-commons"))
    compileOnly("junit:junit:$junitVersion")
    compileOnly("org.testng:testng:$testNgVersion")

    testCompile("io.github.glytching:junit-extensions")
    testCompile("junit:junit:$junitVersion")
    testCompile("org.assertj:assertj-core")
    testCompile("org.junit.jupiter:junit-jupiter-api")
    testCompile("org.junit.jupiter:junit-jupiter-params")
    testCompile("org.mockito:mockito-core")
    testCompile("org.slf4j:slf4j-simple")
    testCompile("org.testng:testng:$testNgVersion")
    testCompile(project(":allure-java-commons-test"))
    testCompile(project(":allure-junit-platform"))
    testRuntime("org.junit.jupiter:junit-jupiter-engine")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.migration"
        ))
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    doFirst {
        jvmArgs("-javaagent:${agent.singleFile}")
    }
}
