description = "Allure OkHttp3 Integration"

dependencies {
    compile(project(":allure-attachments"))
    compile("com.squareup.okhttp3:okhttp")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.okhttp"
        ))
    }
}
