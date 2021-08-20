import io.qameta.allure.gradle.task.AllureReport

val linkHomepage by extra("https://qameta.io/allure")
val linkCi by extra("https://ci.qameta.in/job/allure-java_deploy/")
val linkScmUrl by extra("https://github.com/allure-framework/allure-java")
val linkScmConnection by extra("scm:git:git://github.com/allure-framework/allure-java.git")
val linkScmDevConnection by extra("scm:git:ssh://git@github.com:allure-framework/allure-java.git")

val gradleScriptDir by extra("${rootProject.projectDir}/gradle")
val qualityConfigsDir by extra("$gradleScriptDir/quality-configs")
val spotlessDtr by extra("$qualityConfigsDir/spotless")

val libs = subprojects.filterNot { it.name in "allure-bom" }

tasks.withType(Wrapper::class) {
    gradleVersion = "7.1.1"
}

plugins {
    java
    `java-library`
    `maven-publish`
    signing
    id("com.diffplug.spotless")
    id("io.github.gradle-nexus.publish-plugin")
    id("io.qameta.allure")
    id("io.spring.dependency-management")
    id("ru.vyarus.quality")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}


tasks.withType(JavaCompile::class) {
    options.encoding = "UTF-8"
}

configure(listOf(rootProject)) {
    description = "Allure Java"
    group = "io.qameta.allure"
}

nexusPublishing {
    repositories {
        sonatype()
    }
}

configure(subprojects) {
    group = "io.qameta.allure"
    version = version

    apply(plugin = "signing")
    apply(plugin = "maven-publish")

    publishing {
        publications {
            create<MavenPublication>("maven") {
                suppressAllPomMetadataWarnings()
                versionMapping {
                    allVariants {
                        fromResolutionResult()
                    }
                }
                pom {
                    name.set(project.name)
                    description.set("Module ${project.name} of Allure Framework.")
                    url.set("https://github.com/allure-framework/allure-java")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("baev")
                            name.set("Dmitry Baev")
                            email.set("dmitry.baev@qameta.io")
                        }
                        developer {
                            id.set("eroshenkoam")
                            name.set("Artem Eroshenko")
                            email.set("artem.eroshenko@qameta.io")
                        }
                    }
                    scm {
                        developerConnection.set("scm:git:git://github.com/allure-framework/allure-java")
                        connection.set("scm:git:git://github.com/allure-framework/allure-java")
                        url.set("https://github.com/allure-framework/allure-java")
                    }
                    issueManagement {
                        system.set("GitHub Issues")
                        url.set("https://github.com/allure-framework/allure-java/issues")
                    }
                }
            }
        }
    }

    signing {
        sign(publishing.publications["maven"])
    }

    tasks.withType<Sign>().configureEach {
        onlyIf { !project.version.toString().endsWith("-SNAPSHOT") }
    }

    tasks.withType<GenerateModuleMetadata> {
        enabled = false
    }

    repositories {
        mavenLocal()
        mavenCentral()
    }
}

configure(libs) {
    val project = this
    apply(plugin = "java")
    apply(plugin = "java-library")
    apply(plugin = "io.qameta.allure")
    apply(plugin = "ru.vyarus.quality")
    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "io.spring.dependency-management")

    dependencyManagement {
        imports {
            mavenBom("com.fasterxml.jackson:jackson-bom:2.12.4")
            mavenBom("org.junit:junit-bom:5.7.2")
        }
        dependencies {
            dependency("com.github.tomakehurst:wiremock:2.27.2")
            dependency("com.google.inject:guice:5.0.1")
            dependency("com.google.testing.compile:compile-testing:0.19")
            dependency("com.squareup.retrofit2:retrofit:2.9.0")
            dependency("commons-io:commons-io:2.11.0")
            dependency("io.github.benas:random-beans:3.9.0")
            dependency("io.github.glytching:junit-extensions:2.4.0")
            dependency("org.apache.commons:commons-lang3:3.12.0")
            dependency("org.apache.httpcomponents:httpclient:4.5.13")
            dependency("org.aspectj:aspectjrt:1.9.7")
            dependency("org.aspectj:aspectjweaver:1.9.7")
            dependency("org.assertj:assertj-core:3.20.2")
            dependency("org.codehaus.groovy:groovy-all:2.5.13")
            dependency("org.freemarker:freemarker:2.3.31")
            dependency("org.jboss.resteasy:resteasy-client:4.7.1.Final")
            dependency("org.mock-server:mockserver-netty:5.11.2")
            dependency("org.mockito:mockito-core:3.12.0")
            dependencySet("org.slf4j:1.7.30") {
                entry("slf4j-api")
                entry("slf4j-nop")
                entry("slf4j-simple")
            }
        }
        generatedPomCustomization {
            enabled(false)
        }
    }

    // Excluding shadowed jars from pom.xml https://github.com/gradle/gradle/issues/10861#issuecomment-576562961
    val internal by configurations.creating {
        isVisible = false
        isCanBeConsumed = false
        isCanBeResolved = false
    }
    configurations.compileClasspath.get().extendsFrom(internal)
    configurations.runtimeClasspath.get().extendsFrom(internal)
    configurations.testCompileClasspath.get().extendsFrom(internal)
    configurations.testRuntimeClasspath.get().extendsFrom(internal)

    tasks {
        compileJava {
            options.encoding = "UTF-8"
        }

        compileTestJava {
            options.encoding = "UTF-8"
            options.compilerArgs.add("-parameters")
        }

        jar {
            manifest {
                attributes(mapOf(
                        "Specification-Title" to project.name,
                        "Implementation-Title" to project.name,
                        "Implementation-Version" to project.version
                ))
            }
        }

        test {
            systemProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug")
            systemProperty("allure.model.indentOutput", "true")
            systemProperty("junit.jupiter.execution.parallel.enabled", true)
            systemProperty("junit.jupiter.execution.parallel.mode.default", true)
            testLogging {
                listOf(org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED)
            }
            maxHeapSize = project.property("test.maxHeapSize").toString()
            maxParallelForks = Integer.parseInt(project.property("test.maxParallelForks") as String)
        }

        processTestResources {
            filesMatching("**/allure.properties") {
                filter {
                    it.replace("#project.description#", project.description ?: project.name)
                }
            }
        }
    }

    quality {
        configDir = qualityConfigsDir
        checkstyleVersion = "8.36.1"
        pmdVersion = "6.27.0"
        spotbugsVersion = "4.1.2"
        codenarcVersion = "1.6"
        enabled = !project.hasProperty("disableQuality")
        afterEvaluate {
            val spotbugs = configurations.findByName("spotbugs")
            if (spotbugs != null) {
                dependencies {
                    spotbugs("org.slf4j:slf4j-simple")
                    spotbugs("com.github.spotbugs:spotbugs:4.2.3")
                }
            }
        }
    }

    spotless {
        java {
            target("src/**/*.java")
            removeUnusedImports()
            @Suppress("INACCESSIBLE_TYPE")
            licenseHeaderFile("$spotlessDtr/header.java", "(package|import|open|module|//startfile)")
            endWithNewline()
            replaceRegex("one blank line after package line", "(package .+;)\n+import", "$1\n\nimport")
            replaceRegex("one blank line after import lists", "(import .+;\n\n)\n+", "$1")
        }
        scala {
            target("src/**/*.scala")
            @Suppress("INACCESSIBLE_TYPE")
            licenseHeaderFile("$spotlessDtr/header.java", "(package|//startfile)")
            endWithNewline()
            replaceRegex("one blank line after package line", "(package .+;)\n+import", "$1\n\nimport")
            replaceRegex("one blank line after import lists", "(import .+;\n\n)\n+", "$1")
        }
        groovy {
            target("src/**/*.groovy")
            @Suppress("INACCESSIBLE_TYPE")
            licenseHeaderFile("$spotlessDtr/header.java", "(package|//startfile) ")
            endWithNewline()
            replaceRegex("one blank line after package line", "(package .+;)\n+import", "$1\n\nimport")
            replaceRegex("one blank line after import lists", "(import .+;\n\n)\n+", "$1")
        }
        format("misc") {
            target(
                    "*.gradle",
                    "*.gitignore",
                    "README.md",
                    "CONTRIBUTING.md",
                    "config/**/*.xml",
                    "src/**/*.xml"
            )
            trimTrailingWhitespace()
            endWithNewline()
        }
        encoding("UTF-8")
    }

    allure {
        autoconfigure = false
        aspectjweaver = false
    }

    java {
        withJavadocJar()
        withSourcesJar()
    }

    tasks.withType(Javadoc::class) {
        (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
    }

    publishing.publications.named<MavenPublication>("maven") {
        pom {
            from(components["java"])
        }
    }
}

allure {
    version = "2.13.5"
    autoconfigure = false
    aspectjweaver = false
}

val aggregatedReport by tasks.creating(AllureReport::class) {
    clean = true
    resultsDirs = subprojects.map { file("${it.buildDir}/allure-results") }.filter { it.exists() }
}
