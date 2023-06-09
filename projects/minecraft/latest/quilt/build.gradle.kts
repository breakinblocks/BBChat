@file:Suppress("PropertyName", "UnstableApiUsage")

import org.gradle.util.Path

val minecraft_version: String by project
val parchment_minecraft_version: String by project
val parchment_version: String by project
val quilt_loader_version: String by project
val qsl_version: String by project

plugins {
    id("org.quiltmc.loom")
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
    modImplementation("org.quiltmc:quilt-loader:${quilt_loader_version}")
    modImplementation("org.quiltmc:qsl:${qsl_version}+${minecraft_version}")
    implementation(project(path = ":projects:core", configuration = "shadow"))
    implementation(project(path = vanillaPath))
}

tasks.named<Jar>("jar") {
    archiveClassifier.set("quilt")
}
