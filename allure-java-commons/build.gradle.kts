description = "Allure Java Commons"

val agent by configurations.creating

dependencies {
    agent("org.aspectj:aspectjweaver")
    api("org.slf4j:slf4j-api")
    api(project(":allure-model"))
    compileOnly("org.aspectj:aspectjrt")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("org.apache.tika:tika-core")
    implementation("org.jooq:joor-java-8")
    testImplementation("io.github.benas:random-beans")
    testImplementation("io.github.glytching:junit-extensions")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit-pioneer:junit-pioneer")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation(project(":allure-java-commons-test"))
    testImplementation(project(":allure-junit-platform"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.commons"
        ))
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    doFirst {
        jvmArgs("-javaagent:${agent.singleFile}")
    }
}
