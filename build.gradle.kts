// Top-level build file. Per-module configuration lives in app/build.gradle.kts.
// Kotlin itself comes from AGP 9's built-in Kotlin support; only the Compose
// compiler plugin is applied separately.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
