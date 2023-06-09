@file:Suppress("PropertyName", "UnstableApiUsage")

import org.gradle.util.Path

val minecraft_version: String by project
val parchment_minecraft_version: String by project
val parchment_version: String by project
val fabric_loader_version: String by project
val fabric_api_version: String by project

plugins {
    id("fabric-loom")
}

val parentPath = Path.path(project.path).parent!!
val vanillaPath = parentPath.child("vanilla").path!!
evaluationDependsOn(vanillaPath)

dependencies {
    "minecraft"("com.mojang:minecraft:${minecraft_version}")
    "mappings"(loom.layered {
        this.officialMojangMappings { nameSyntheticMembers = false }
        this.parchment("org.parchmentmc.data:parchment-${parchment_minecraft_version}:${parchment_version}@zip")
    })
    modImplementation("net.fabricmc:fabric-loader:${fabric_loader_version}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${fabric_api_version}")
    implementation(project(path = ":projects:core", configuration = "shadow"))
    implementation(project(path = vanillaPath))
}

tasks.named<Jar>("jar") {
    archiveClassifier.set("quilt")
}
