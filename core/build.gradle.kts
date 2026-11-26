plugins {
    alias(libs.plugins.kotlin.jvm)
}

version = "1.2"

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
}
