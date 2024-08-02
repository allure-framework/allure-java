description = "Allure JUnit 4 Aspect HACK"

val junitVersion = "4.13.2"

dependencies {
    api(project(":allure-junit4"))
    compileOnly("junit:junit:$junitVersion")
    compileOnly("org.aspectj:aspectjrt")
}

tasks.jar {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.junit4aspect"
        ))
    }
}
