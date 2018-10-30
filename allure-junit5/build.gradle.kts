description = "Allure JUnit 5 Integration"

val agent by configurations.creating

dependencies {
    agent("org.aspectj:aspectjweaver")
    compile("org.junit.jupiter:junit-jupiter-engine")
    compile(project(":allure-java-commons"))
    compile(project(":allure-junit-platform"))
    testCompile("org.assertj:assertj-core")
    testCompile("org.junit.jupiter:junit-jupiter-api")
    testCompile("org.junit.jupiter:junit-jupiter-params")
    testCompile("org.slf4j:slf4j-simple")
    testCompile(project(":allure-java-commons-test"))
    testRuntime("org.junit.jupiter:junit-jupiter-engine")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.junit5"
        ))
    }
    from("src/main/services") {
        into("META-INF/services")
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    exclude("**/samples/*")
    doFirst {
        jvmArgs("-javaagent:${agent.singleFile}")
    }
}

val spiOffJar by tasks.creating(Jar::class) {
    from(sourceSets.getByName("main").output)
    classifier = "sources"
}

val spiOff by configurations.creating {
    extendsFrom(configurations.getByName("compile"))
}

artifacts.add("archives", spiOffJar)
artifacts.add("spiOff", spiOffJar)
