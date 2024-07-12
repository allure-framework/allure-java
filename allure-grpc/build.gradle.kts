import com.google.protobuf.gradle.*

plugins {
    id("com.google.protobuf")
}

description = "Allure gRPC Integration"

val agent: Configuration by configurations.creating

val grpcVersion = "1.65.1"
val protobufVersion = "4.27.2"

dependencies {
    agent("org.aspectj:aspectjweaver")
    api(project(":allure-attachments"))
    implementation("io.grpc:grpc-core:$grpcVersion")
    implementation("com.google.protobuf:protobuf-java-util:$protobufVersion")

    testImplementation("io.grpc:grpc-stub:$grpcVersion")
    testImplementation("io.grpc:grpc-protobuf:$grpcVersion")
    testImplementation("io.grpc:grpc-netty-shaded:$grpcVersion")
    testImplementation("com.google.protobuf:protobuf-java:$protobufVersion")
    testImplementation("org.grpcmock:grpcmock-junit5")
    testImplementation("javax.annotation:javax.annotation-api")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation(project(":allure-java-commons-test"))
    testImplementation(project(":allure-junit-platform"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.jar {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.grpc"
        ))
    }
}

tasks.test {
    useJUnitPlatform()
    doFirst {
        jvmArgs("-javaagent:${agent.singleFile}")
    }
}

sourceSets {
    test {
        java {
            srcDir("build/generated/source/proto/test")
            srcDir("src/test/java")
        }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
        }
    }
    generateProtoTasks {
        ofSourceSet("test").forEach {
            it.plugins {
                id("grpc")
            }
        }
    }
}
