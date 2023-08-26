@file:Suppress("PropertyName")

import org.spongepowered.gradle.vanilla.MinecraftExtension

val minecraft_version: String by project
val parchment_minecraft_version: String by project
val parchment_version: String by project
val forge_config_api_port_version: String by project

plugins {
    `java-library`
    id("org.spongepowered.gradle.vanilla")
}

configure<MinecraftExtension> {
    version(minecraft_version)
}

dependencies {
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation(project(path = ":projects:core", configuration = "shadow"))
}

tasks.jar {
    archiveClassifier.set("vanilla")
}
