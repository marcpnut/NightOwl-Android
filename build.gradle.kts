buildscript {
    dependencies {
        // AGP 9 supplies Kotlin support; this pins its compiler to the Compose compiler plugin version.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.10")
    }
}

plugins {
    id("com.android.application") version "9.4.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
}
