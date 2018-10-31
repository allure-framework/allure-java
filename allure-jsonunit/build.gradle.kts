description = "Allure JsonUnit Integration"

dependencies {
    compile("org.apache.commons:commons-lang3")
    compile("net.javacrumbs.json-unit:json-unit:2.0.0.RC1")
    compile(project(":allure-attachments"))
    testCompile("junit:junit")
    testCompile("org.hamcrest:hamcrest-library")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.jsonunit"
        ))
    }
}
