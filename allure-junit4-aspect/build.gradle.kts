description = "Allure JUnit 4 Aspect HACK"

val agent by configurations.creating

dependencies {
    agent("org.aspectj:aspectjweaver")
    compile(project(":allure-junit4"))
    compileOnly("junit:junit")
    compileOnly("org.aspectj:aspectjrt")
    testCompile("junit:junit")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.junit4aspect"
        ))
    }
}
