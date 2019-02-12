description = "Allure JUnit 4 Aspect HACK"

val agent by configurations.creating

val junitVersion = "4.12"

dependencies {
    agent("org.aspectj:aspectjweaver")
    api(project(":allure-junit4"))
    implementation("junit:junit:$junitVersion")
    implementation("org.aspectj:aspectjrt")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.junit4aspect"
        ))
    }
}
