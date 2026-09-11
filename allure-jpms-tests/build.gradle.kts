description = "Allure JPMS End-to-End Tests"

// JARs are fixture inputs, separate from the JUnit process that runs the assertions.
val consumerJars = configurations.create("consumerJars") {
    isCanBeConsumed = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
    }
}

// Verify the packaged artifacts, including metadata contributed by Jar and SPI-off tasks.
val publishedJars = configurations.create("publishedJars") {
    isCanBeConsumed = false
    isTransitive = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
    }
}

dependencies {
    publishedJars(project(":allure-assertj"))
    publishedJars(project(":allure-awaitility"))
    publishedJars(project(":allure-cucumber7-jvm"))
    publishedJars(project(":allure-descriptions-javadoc"))
    publishedJars(project(":allure-grpc"))
    publishedJars(project(":allure-hamcrest"))
    publishedJars(project(":allure-httpclient"))
    publishedJars(project(":allure-httpclient5"))
    publishedJars(project(":allure-java-commons"))
    publishedJars(project(":allure-java-commons-test"))
    publishedJars(project(":allure-java-httpclient"))
    publishedJars(project(":allure-jax-rs"))
    publishedJars(project(":allure-jbehave5"))
    publishedJars(project(":allure-jsonunit"))
    publishedJars(project(":allure-junit-platform"))
    publishedJars(project(":allure-junit4"))
    publishedJars(project(":allure-junit4-aspect"))
    publishedJars(project(":allure-jupiter"))
    publishedJars(project(":allure-jupiter-assert"))
    publishedJars(project(":allure-kotlin-coroutines"))
    publishedJars(project(":allure-kotlin-extensions"))
    publishedJars(project(":allure-model"))
    publishedJars(project(":allure-okhttp3"))
    publishedJars(project(":allure-playwright"))
    publishedJars(project(":allure-rest-assured"))
    publishedJars(project(":allure-selenium-bidi"))
    publishedJars(project(":allure-servlet-api"))
    publishedJars(project(":allure-spock2"))
    publishedJars(project(":allure-spring-web"))
    publishedJars(project(":allure-testng"))
    if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_21)) {
        publishedJars(project(":allure-jooq"))
    }

    consumerJars(project(":allure-jupiter"))
    consumerJars(project(":allure-testng"))
    consumerJars(project(":allure-java-httpclient"))
    consumerJars(project(":allure-junit4"))
    consumerJars(project(":allure-jsonunit"))
    consumerJars(project(":allure-kotlin-coroutines"))
    consumerJars(project(":allure-spock2"))
    consumerJars(project(":allure-descriptions-javadoc"))
    consumerJars("junit:junit")
    consumerJars("net.javacrumbs.json-unit:json-unit:5.1.2")
    // Exercise JsonUnit with either consumer-selected JSON provider.
    consumerJars("com.fasterxml.jackson.core:jackson-databind")
    consumerJars("com.google.code.gson:gson:2.11.0")
    consumerJars("com.google.errorprone:error_prone_annotations:2.27.0")
    consumerJars("org.spockframework:spock-core:2.4-groovy-5.0")
    consumerJars("org.apache.groovy:groovy:5.1.2")
    consumerJars("org.junit.jupiter:junit-jupiter-engine")
    consumerJars("org.junit.jupiter:junit-jupiter-params")
    // javac needs these static JUnit dependencies as well as its runtime dependencies.
    consumerJars("org.apiguardian:apiguardian-api:1.1.2")
    consumerJars("org.jspecify:jspecify:1.0.0")
    consumerJars("org.testng:testng:7.12.0")
    consumerJars("org.slf4j:slf4j-simple")
    consumerJars("org.aspectj:aspectjrt")
    consumerJars("org.aspectj:aspectjweaver")

    testImplementation(project(":allure-java-commons"))
    testImplementation(project(":allure-assertj"))
    testImplementation("com.fasterxml.jackson.core:jackson-databind")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly(project(":allure-junit-platform"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.processTestResources {
    from(publishedJars) {
        into("published-jars")
    }
    from(consumerJars) {
        into("jars")
    }
    val adapters = listOf("allure-junit-platform", "allure-jupiter", "allure-testng", "allure-spock2")
    dependsOn(adapters.map { ":$it:spiOffJar" })
    from(provider {
        adapters.map { project(":$it").tasks.named<Jar>("spiOffJar").get().archiveFile.get() }
    }) {
        into("jars")
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    enabled = false
}
