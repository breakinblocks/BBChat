@file:Suppress("PropertyName")
import org.spongepowered.gradle.vanilla.MinecraftExtension
val minecraft_version: String by project
val parchment_minecraft_version: String by project
val parchment_version: String by project

plugins {
    id("org.spongepowered.gradle.vanilla")
}

configure<MinecraftExtension> {
    version(minecraft_version)
}

dependencies {
    implementation(project(path = ":projects:core", configuration = "shadow"))
}

tasks.named<Jar>("jar") {
    archiveClassifier.set("vanilla")
}
