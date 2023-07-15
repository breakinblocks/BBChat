@file:Suppress("PropertyName", "UnstableApiUsage")

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.task.RemapJarTask
import org.gradle.util.Path

val mod_version: String by project
val minecraft_version: String by project
val parchment_minecraft_version: String by project
val parchment_version: String by project
val quilt_loader_version: String by project
val qsl_version: String by project
val forge_config_api_port_version: String by project

plugins {
    id("com.github.johnrengelman.shadow")
    id("org.quiltmc.loom")
    id("bbchat")
}

val parentPath = Path.path(project.path).parent!!.path!!
val vanillaPath = Path.path(parentPath).child("vanilla").path!!
evaluationDependsOn(vanillaPath)

dependencies {
    "minecraft"("com.mojang:minecraft:${minecraft_version}")
    "mappings"(loom.layered {
        this.officialMojangMappings { nameSyntheticMembers = false }
        this.parchment("org.parchmentmc.data:parchment-${parchment_minecraft_version}:${parchment_version}@zip")
    })
    modImplementation("org.quiltmc:quilt-loader:${quilt_loader_version}")
    modImplementation("org.quiltmc:qsl:${qsl_version}+${minecraft_version}")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation(project(path = ":projects:core", configuration = "shadow"))
    implementation(project(path = vanillaPath))
    modApi("fuzs.forgeconfigapiport:forgeconfigapiport-fabric:${forge_config_api_port_version}")
    include("fuzs.forgeconfigapiport:forgeconfigapiport-fabric:${forge_config_api_port_version}")
}

tasks.named<ProcessResources>("processResources") {
    from(project(vanillaPath).sourceSets.main.get().resources)
    inputs.property("mod_version", mod_version)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    from(sourceSets["main"].resources.srcDirs) {
        include("quilt.mod.json")
        expand(
            "mod_version" to mod_version
        )
    }
    from(sourceSets["main"].resources.srcDirs) {
        exclude("quilt.mod.json")
    }
}

tasks.withType<JavaCompile> {
    source(project(vanillaPath).sourceSets.main.get().allSource)
}

val remapJarMutexPath = bbchat.getMutexDir(minecraft_version, "remapJar")

tasks.withType<RemapJarTask> {
    // This is to prevent multiple of this task running at the same time in parallel builds.
    outputs.dir(remapJarMutexPath)
}

tasks.named<Jar>("jar") {
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

val shadowJar = tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set(project.name)
    dependencies {
        include(project(":projects:core"))
    }
}

tasks.named<DefaultTask>("build") {
    dependsOn(shadowJar)
}
