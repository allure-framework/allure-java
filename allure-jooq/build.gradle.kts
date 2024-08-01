description = "Allure JOOQ Integration"

val jooqVersion = "3.19.10"

dependencies {
    api(project(":allure-java-commons"))
    implementation("org.jooq:jooq:${jooqVersion}")
    testImplementation("io.zonky.test:embedded-postgres:2.0.7")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation(platform("io.zonky.test.postgres:embedded-postgres-binaries-bom:16.2.0"))
    testImplementation(project(":allure-assertj"))
    testImplementation(project(":allure-java-commons-test"))
    testImplementation(project(":allure-junit-platform"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.postgresql:postgresql:42.7.3")
}

tasks.compileJava {
    options.release.set(17)
}

tasks.jar {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.jooq"
        ))
    }
}

tasks.test {
    useJUnitPlatform()
}
