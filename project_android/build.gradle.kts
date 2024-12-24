// Top-level build file where you can add configuration options common to all sub-projects/modules.
android {
    lint {
        baseline = file("lint-baseline.xml")
        abortOnError = false // 遇到錯誤不阻止構建
    }
}
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}
