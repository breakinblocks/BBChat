@file:Suppress("PropertyName", "UnstableApiUsage")

val minecraft_version: String by project
val parchment_minecraft_version: String by project
val parchment_version: String by project

plugins {
    id("org.quiltmc.loom")
}

dependencies {
    implementation(project(path = ":projects:core", configuration = "shadow"))
    "minecraft"("com.mojang:minecraft:${minecraft_version}")
    "mappings"(loom.layered {
        this.officialMojangMappings { nameSyntheticMembers = false }
        this.parchment("org.parchmentmc.data:parchment-${parchment_minecraft_version}:${parchment_version}@zip")
    })
}

tasks.named<Jar>("jar") {
    archiveClassifier.set("vanilla")
}
