description = "Allure Citrus Integration"

val agent by configurations.creating

val citrusVersion = "2.7.8"

dependencies {
    agent("org.aspectj:aspectjweaver")
    compile("com.consol.citrus:citrus-core:$citrusVersion")
    compile(project(":allure-java-commons"))
    testCompile("com.consol.citrus:citrus-http:$citrusVersion")
    testCompile("com.consol.citrus:citrus-java-dsl:$citrusVersion")
    testCompile("io.github.glytching:junit-extensions")
    testCompile("org.assertj:assertj-core")
    testCompile("org.junit.jupiter:junit-jupiter-api")
    testCompile("org.junit.jupiter:junit-jupiter-params")
    testCompile("org.slf4j:slf4j-simple")
    testCompile(project(":allure-assertj"))
    testCompile(project(":allure-java-commons-test"))
    testCompile(project(":allure-junit-platform"))
    testRuntime("org.junit.jupiter:junit-jupiter-engine")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.citrus"
        ))
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    exclude("**/samples/*")
    doFirst {
        jvmArgs("-javaagent:${agent.singleFile}")
    }
}
