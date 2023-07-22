@file:Suppress("PropertyName", "UnstableApiUsage")

import net.fabricmc.loom.task.RemapJarTask
import org.gradle.util.Path

val mod_version: String by project
val minecraft_version: String by project
val parchment_minecraft_version: String by project
val parchment_version: String by project
val fabric_loader_version: String by project
val fabric_api_version: String by project
val forge_config_api_port_version: String by project

plugins {
    id("com.github.johnrengelman.shadow")
    id("fabric-loom")
}

val corePath = ":projects:core"
val parentPath = Path.path(project.path).parent!!.path!!
val vanillaPath = Path.path(parentPath).child("vanilla").path!!
evaluationDependsOn(vanillaPath)

val includeModApi: Configuration by configurations.creating

configurations {
    configurations.include {
        extendsFrom(includeModApi)
    }
    configurations.modApi {
        extendsFrom(includeModApi)
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${minecraft_version}")
    mappings(loom.layered {
        this.officialMojangMappings { nameSyntheticMembers = false }
        this.parchment("org.parchmentmc.data:parchment-${parchment_minecraft_version}:${parchment_version}@zip")
    })
    modImplementation("net.fabricmc:fabric-loader:${fabric_loader_version}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${fabric_api_version}") {
        exclude("net.fabricmc", "fabric-loader")
    }
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation(project(path = corePath, configuration = "shadow"))
    compileOnly(project(path = vanillaPath))
    includeModApi("net.minecraftforge:forgeconfigapiport-fabric:${forge_config_api_port_version}") {
        exclude("net.fabricmc")
        exclude("net.fabricmc.fabric-api")
    }
    compileOnly("com.electronwill.night-config:core:3.6.3")
    compileOnly("com.electronwill.night-config:toml:3.6.3")
}

tasks.withType<JavaCompile> {
    source(project(vanillaPath).sourceSets.main.get().allSource)
}

tasks.processResources {
    from(project(vanillaPath).sourceSets.main.get().resources)
    inputs.property("mod_version", mod_version)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    from(sourceSets["main"].resources.srcDirs) {
        include("fabric.mod.json")
        expand(
            "mod_version" to mod_version
        )
    }
    from(sourceSets["main"].resources.srcDirs) {
        exclude("fabric.mod.json")
    }
}

val remapJarMutexPath = bbchat.getMutexDir(minecraft_version, "remapJar")

tasks.withType<RemapJarTask> {
    // This is to prevent multiple of this task running at the same time in parallel builds.
    outputs.dir(remapJarMutexPath)
}

tasks.jar {
    manifest {
        attributes(
            "Specification-Title" to "BBChat",
            "Specification-Vendor" to "Breakin' Blocks",
            "Specification-Version" to "1",
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "Breakin' Blocks"
        )
    }
}

tasks.shadowJar {
    dependencies {
        include(project(corePath))
    }

    filesMatching("fabric.mod.json") {
        filter {
            it.replace(
                "\"depends\": {", """
                "depends": {
                "com_electronwill_night-config_core": "*",
                "com_electronwill_night-config_toml": "*",
            """.trimIndent()
            )
        }
    }
}

tasks.remapJar {
    archiveClassifier.set(project.name)
    dependsOn(tasks.shadowJar)
    inputFile.set(tasks.shadowJar.get().archiveFile)
}
