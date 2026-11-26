plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

version = "1.2"

kotlin {
    jvmToolchain(21)
}

dependencies {
    api(project(":core"))
    api(project(":panchanga-domain"))
    implementation(libs.cosinekitty.astronomy)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
}
