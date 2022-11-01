@file:Suppress("PropertyName")

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.kyori.blossom.BlossomExtension
import net.minecraftforge.gradle.userdev.UserDevExtension
import net.minecraftforge.gradle.userdev.tasks.RenameJarInPlace

val mod_version: String by project
val mc_version: String by project
val mc_version_range_supported: String by project
val forge_version: String by project
val forge_version_range_supported: String by project
val mappings_channel: String by project
val mappings_version: String by project

plugins {
    id("com.github.ben-manes.versions")
    id("com.github.johnrengelman.shadow")
    id("net.kyori.blossom")
    id("net.minecraftforge.gradle")
}

base.archivesName.set("bbchat-${mc_version}")

java.toolchain.languageVersion.set(JavaLanguageVersion.of(8))

configure<UserDevExtension> {
    mappings(mappings_channel, mappings_version)
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
                    sources = listOf(sourceSets["main"])
                }
            }
        }
        create("server") {
            workingDirectory(file("run"))
            property("forge.logging.markers", "SCAN,REGISTRIES,REGISTRYDUMP")
            property("forge.logging.console.level", "debug")
            mods {
                create("bbchat") {
                    sources = listOf(sourceSets["main"])
                }
            }
        }
    }
}

dependencies {
    add("minecraft", "net.minecraftforge:forge:${mc_version}-${forge_version}")
    implementation(project(path = ":bbchat-common", configuration = "shadow"))
}

configure<BlossomExtension> {
    replaceToken("version = \"\"", "version = \"${mod_version}\"")
    replaceToken("dependencies = \"\"", "dependencies = \"required-after:forge@${forge_version_range_supported};\"")
    replaceToken("acceptedMinecraftVersions = \"\"", "acceptedMinecraftVersions = \"${mc_version_range_supported}\"")
    replaceTokenIn("/BBChat.java")
}

tasks.named<ProcessResources>("processResources") {
    inputs.property("mod_version", mod_version)
    inputs.property("mc_version", mc_version)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    from(sourceSets["main"].resources.srcDirs) {
        include("mcmod.info")
        expand(
            "mod_version" to mod_version,
            "mc_version" to mc_version
        )
    }
    from(sourceSets["main"].resources.srcDirs) {
        exclude("mcmod.info")
    }
}

val shadowJar = tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("forge")
    dependencies {
        include(project(":bbchat-common"))
    }
    exclude("dummyThing")
}

extensions.configure<NamedDomainObjectContainer<RenameJarInPlace>> {
    create("shadowJar")
}

tasks.named<DefaultTask>("build") {
    dependsOn(shadowJar)
}
