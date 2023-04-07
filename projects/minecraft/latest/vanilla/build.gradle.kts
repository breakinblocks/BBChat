@file:Suppress("PropertyName")

import org.spongepowered.gradle.vanilla.MinecraftExtension

val mc_version: String by project

plugins {
    id("com.github.ben-manes.versions")
    `java`
    id("org.spongepowered.gradle.vanilla")
}

base.archivesName.set("bbchat-${mc_version}")

java.toolchain.languageVersion.set(JavaLanguageVersion.of(17))

configure<MinecraftExtension> {
    version(mc_version)
}

dependencies {
    implementation(project(path = ":projects:core", configuration = "shadow"))
}

tasks.named<Jar>("jar") {
    archiveClassifier.set("vanilla")
}
