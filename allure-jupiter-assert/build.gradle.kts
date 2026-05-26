description = "Allure Jupiter Assertions Integration"

dependencies {
    api(project(":allure-jupiter"))
    compileOnly("org.aspectj:aspectjrt")
    compileOnly("org.junit.jupiter:junit-jupiter-api")
    testAnnotationProcessor(project(":allure-descriptions-javadoc"))
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation(project(":allure-java-commons-test"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.jar {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.jupiterassert"
        ))
    }
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("legacyJunit5Assert") {
            artifactId = "allure-junit5-assert"
            pom {
                packaging = "pom"
                distributionManagement {
                    relocation {
                        groupId.set(project.group.toString())
                        artifactId.set("allure-jupiter-assert")
                        version.set(project.version.toString())
                        message.set("allure-junit5-assert has been renamed to allure-jupiter-assert.")
                    }
                }
            }
        }
    }
}
