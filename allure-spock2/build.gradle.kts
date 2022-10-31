description = "Allure Spock 2 Framework Integration"

plugins {
    groovy
}

val spockFrameworkVersion = "2.3-groovy-3.0"
val groovyVersion = "3.0.13"

dependencies {
    api(project(":allure-java-commons"))
    implementation("org.spockframework:spock-core:$spockFrameworkVersion")
    compileOnly("org.aspectj:aspectjrt")
    implementation(project(":allure-test-filter"))
    testAnnotationProcessor("org.slf4j:slf4j-simple")
    testAnnotationProcessor(project(":allure-descriptions-javadoc"))
    testImplementation("io.github.glytching:junit-extensions")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.codehaus.groovy:groovy:${groovyVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation(project(":allure-assertj"))
    testImplementation(project(":allure-java-commons-test"))
    testImplementation(project(":allure-junit-platform"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.jar {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.spock2"
        ))
    }

    from("src/main/services") {
        into("META-INF/services")
    }
}

tasks.test {
    useJUnitPlatform()
    exclude("**/samples/*")
}

val spiOffJar: Jar by tasks.creating(Jar::class) {
    from(sourceSets.getByName("main").output)
    archiveClassifier.set("spi-off")
}

publishing {
    publications {
        named<MavenPublication>("maven") {
            artifact(spiOffJar)
        }
    }
}

