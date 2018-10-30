description = "Allure JUnit Platform Integration"

val agent by configurations.creating

dependencies {
    agent("org.aspectj:aspectjweaver")
    compile(project(":allure-java-commons"))
    compile("org.junit.jupiter:junit-jupiter-api")
    compile("org.junit.platform:junit-platform-launcher")
    testCompile(project(":allure-java-commons-test"))
    testCompile("org.assertj:assertj-core")
    testCompile("org.junit.jupiter:junit-jupiter-api")
    testCompile("org.junit.jupiter:junit-jupiter-params")
    testCompile("org.slf4j:slf4j-simple")
    testRuntime("org.junit.jupiter:junit-jupiter-engine")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.junitplatform"
        ))
    }
    from("src/main/services") {
        into("META-INF/services")
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    exclude("**/features/*")
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
