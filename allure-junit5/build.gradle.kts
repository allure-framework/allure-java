description = "Allure JUnit 5 Integration"

val agent by configurations.creating

dependencies {
    agent("org.aspectj:aspectjweaver")
    compile(project(":allure-junit-platform"))
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
    classifier = "spi-off"
}

val spiOff by configurations.creating {
    extendsFrom(configurations.getByName("compile"))
}

artifacts.add("archives", spiOffJar)
artifacts.add("spiOff", spiOffJar)
