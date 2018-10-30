description = "Allure Rest-Assured Integration"

dependencies {
    compile(project(":allure-attachments"))
    compile("io.rest-assured:rest-assured")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.restassured"
        ))
    }
}
