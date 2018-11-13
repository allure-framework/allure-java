description = "Allure Java Migration Utils"

val agent by configurations.creating

dependencies {
    agent("org.aspectj:aspectjweaver")
    compile("org.apache.commons:commons-lang3")
    compile("org.aspectj:aspectjrt")
    compile(project(":allure-java-commons"))
    compileOnly("junit:junit")
    compileOnly("org.testng:testng")
    testCompile("junit:junit")
    testCompile("org.assertj:assertj-core")
    testCompile("org.mockito:mockito-core")
    testCompile("org.slf4j:slf4j-simple")
    testCompile("org.testng:testng")
    testCompile(project(":allure-java-commons-test"))
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.migration"
        ))
    }
}

tasks.named<Test>("test") {
    doFirst {
        jvmArgs("-javaagent:${agent.singleFile}")
    }
}
