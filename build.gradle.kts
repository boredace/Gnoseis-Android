// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
//        classpath("org.jetbrains.kotlin:kotlin-serialization:2.0.0")
        classpath(kotlin("gradle-plugin", version = "1.9.22"))

    }
}
plugins {
    id("com.android.application") version "8.7.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.google.devtools.ksp") version "1.9.21-1.0.15" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.22" apply false

}