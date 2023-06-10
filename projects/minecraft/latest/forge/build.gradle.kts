@file:Suppress("PropertyName")

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.minecraftforge.gradle.userdev.UserDevExtension
import net.minecraftforge.gradle.userdev.tasks.RenameJarInPlace
import org.gradle.util.Path

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

val parentPath = Path.path(project.path).parent!!
val vanillaPath = parentPath.child("vanilla").path!!
evaluationDependsOn(vanillaPath)

configure<UserDevExtension> {
    mappings("parchment", "${parchment_version}-${parchment_minecraft_version}")
    runs {
        all {
            lazyToken("minecraft_classpath") {
                val configurationCopy = configurations.implementation.get().copyRecursive()
                configurationCopy.isCanBeResolved = true
                configurationCopy.isTransitive = false
                configurationCopy.resolve().joinToString(File.pathSeparator) { it.absolutePath }
            }
        }
        create("client") {
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
        create("server") {
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
        create("data") {
            workingDirectory(file("run"))
            property("forge.logging.markers", "SCAN,REGISTRIES,REGISTRYDUMP")
            property("forge.logging.console.level", "debug")
            setArgs(listOf("--mod", "bbchat", "--all", "--output", file("src/generated/resources/")))
            mods {
                create("bbchat") {
                    sources = listOf(
                        sourceSets["main"],
                        project(vanillaPath).sourceSets["main"],
                    )
                }
            }
        }
    }
}

dependencies {
    "minecraft"("net.minecraftforge:forge:${minecraft_version}-${forge_version}")
    implementation(project(path = ":projects:core", configuration = "shadow"))
    implementation(project(path = vanillaPath))
}

tasks.withType<JavaCompile> {
    source(project(vanillaPath).sourceSets["main"].allSource)
}

tasks.named<ProcessResources>("processResources") {
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

tasks.withType<JavaCompile> {
    source(project(vanillaPath).sourceSets.main.get().allSource)
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

extensions.configure<NamedDomainObjectContainer<RenameJarInPlace>> {
    create("shadowJar")
}

tasks.named<DefaultTask>("build") {
    dependsOn(shadowJar)
}
