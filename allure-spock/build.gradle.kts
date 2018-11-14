import groovy.sql.GroovyResultSetExtension

description = "Allure Spock Framework Integration"

apply(plugin = "groovy")

val agent by configurations.creating

val spockFrameworkVersion = "1.1-groovy-2.4"

dependencies {
    agent("org.aspectj:aspectjweaver")
    compile("org.spockframework:spock-core:$spockFrameworkVersion")
    compile(project(":allure-java-commons"))
    testCompile("org.assertj:assertj-core")
    testCompile("org.codehaus.groovy:groovy-all")
    testCompile("org.mockito:mockito-core")
    testCompile("org.slf4j:slf4j-simple")
    testCompile(project(":allure-junit-platform"))
    testCompile(project(":allure-java-commons-test"))
    testRuntime("org.junit.jupiter:junit-jupiter-engine")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.spock"
        ))
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    exclude("**/samples/*")
    doFirst {
        jvmArgs("-javaagent:${agent.singleFile}")
    }
}

//
//val sourceSets = project.the<SourceSetContainer>()
//sourceSets.getByName("test") {
//    java {
//        srcDir("src/test/groovy")
//    }
//}

//sourceSets.getByName("test").withGroovyBuilder {  }

