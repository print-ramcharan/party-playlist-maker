// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.1.1" apply false
    id("org.jetbrains.kotlin.android") version "1.8.10" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}
buildscript {

    dependencies {
        classpath("com.android.tools.build:gradle:8.1.1") // Example version, use the correct one for your project
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.10") // Example version, use the correct one for your project
        classpath("com.google.gms:google-services:4.4.2")
    }
}
