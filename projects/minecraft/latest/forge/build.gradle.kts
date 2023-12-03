@file:Suppress("PropertyName")

import net.minecraftforge.gradle.userdev.UserDevExtension
import net.minecraftforge.gradle.userdev.tasks.RenameJarInPlace
import org.gradle.util.Path

val mod_id: String by project
val mod_version: String by project
val minecraft_version: String by project
val minecraft_version_range_supported: String by project
val forge_version: String by project
val forge_version_range_supported: String by project
val parchment_minecraft_version: String by project
val parchment_version: String by project

plugins {
    id("com.github.ben-manes.versions")
    id("com.github.johnrengelman.shadow")
    id("net.minecraftforge.gradle")
    id("org.parchmentmc.librarian.forgegradle")
}

val corePath = ":projects:core"
val parentPath = Path.path(project.path).parent!!.path!!
val vanillaPath = Path.path(parentPath).child("vanilla").path!!
evaluationDependsOn(vanillaPath)

configure<UserDevExtension> {
    mappings("parchment", "${parchment_version}-${parchment_minecraft_version}")
    copyIdeResources.set(true)
    runs {
        configureEach {
            workingDirectory(file("run"))
            property("forge.logging.markers", "SCAN,REGISTRIES,REGISTRYDUMP")
            property("forge.logging.console.level", "debug")
            mods {
                create("bbchat") {
                    sources = listOf(
                        sourceSets["main"],
                        project(vanillaPath).sourceSets["main"],
                    )
                }
            }
        }
        create("client") {
            property("forge.enabledGameTestNamespaces", mod_id)
        }
        create("server") {
            property("forge.enabledGameTestNamespaces", mod_id)
        }
        create("gameTestServer") {
            property("forge.enabledGameTestNamespaces", mod_id)
        }
        create("data") {
            workingDirectory(file("run"))
            setArgs(
                listOf(
                    "--mod", "bbchat",
                    "--all",
                    "--output", file("src/generated/resources/"),
                    "--existing", file("src/main/resources/"),
                )
            )
        }
    }
}

dependencies {
    minecraft("net.minecraftforge:forge:${minecraft_version}-${forge_version}")
    minecraftLibrary(project(path = corePath, configuration = "shadow"))
    compileOnly(project(path = vanillaPath))
}

tasks.withType<JavaCompile> {
    source(project(vanillaPath).sourceSets.main.get().allSource)
}

tasks.processResources {
    from(project(vanillaPath).sourceSets.main.get().resources)
    inputs.property("mod_version", mod_version)
    inputs.property("minecraft_version_range_supported", minecraft_version_range_supported)
    inputs.property("forge_version_range_supported", forge_version_range_supported)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    from(sourceSets["main"].resources.srcDirs) {
        include("META-INF/mods.toml")
        expand(
            "mod_version" to mod_version,
            "minecraft_version_range_supported" to minecraft_version_range_supported,
            "forge_version_range_supported" to forge_version_range_supported
        )
    }
    from(sourceSets["main"].resources.srcDirs) {
        exclude("META-INF/mods.toml")
    }
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
    archiveClassifier.set(project.name)
    dependencies {
        include(project(corePath))
    }
}

extensions.configure<NamedDomainObjectContainer<RenameJarInPlace>> {
    create("shadowJar")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
