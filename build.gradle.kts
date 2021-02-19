@file:Suppress("PropertyName")

val mod_version: String by project

allprojects {
    group = "com.breakinblocks.bbchat"
    version = mod_version
}

subprojects {
    buildscript {
        repositories {
            jcenter()
            mavenCentral()
        }
        dependencies {
            classpath("com.github.jengelman.gradle.plugins:shadow:5.2.0")
        }
    }

    apply(plugin = "java")
    apply(plugin = "eclipse")
    apply(plugin = "idea")

    configure<JavaPluginConvention> {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    repositories {
        jcenter()
        mavenCentral()
    }
}
