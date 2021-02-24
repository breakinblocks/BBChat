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
    id("com.github.johnrengelman.shadow")
    id("net.kyori.blossom")
    id("net.minecraftforge.gradle")
}

base.archivesBaseName = "bbchat-${mc_version}"

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

configure<UserDevExtension> {
    mappings(mappings_channel, mappings_version)

    runs {
        create("client") {
            workingDirectory(file("run"))

            // Recommended logging data for a userdev environment
            property("forge.logging.markers", "SCAN,REGISTRIES,REGISTRYDUMP")

            // Recommended logging level for the console
            property("forge.logging.console.level", "debug")

            mods {
                create("bbchat") {
                    sources = listOf(sourceSets["main"])
                }
            }
        }

        create("server") {
            workingDirectory(file("run"))

            // Recommended logging data for a userdev environment
            property("forge.logging.markers", "SCAN,REGISTRIES,REGISTRYDUMP")

            // Recommended logging level for the console
            property("forge.logging.console.level", "debug")

            mods {
                create("bbchat") {
                    sources = listOf(sourceSets["main"])
                }
            }
        }
    }
}

configure<BlossomExtension> {
    replaceToken("version = \"\"", "version = \"${mod_version}\"")
    replaceToken("dependencies = \"\"", "dependencies = \"required-after:forge@${forge_version_range_supported};\"")
    replaceToken("acceptedMinecraftVersions = \"\"", "acceptedMinecraftVersions = \"${mc_version_range_supported}\"")
    replaceTokenIn("/BBChat.java")
}

dependencies {
    add("minecraft", "net.minecraftforge:forge:${mc_version}-${forge_version}")
    implementation(project(path = ":bbchat-common", configuration = "shadow"))
}

tasks.named<ProcessResources>("processResources") {
    // this will ensure that this task is redone when the versions change.
    inputs.property("mod_version", mod_version)
    inputs.property("mc_version", mc_version)

    // replace stuff in mcmod.info, nothing else
    from(sourceSets["main"].resources.srcDirs) {
        include("mcmod.info")

        // replace mod_version and mc_version_range_supported and forge_version_major
        expand("mod_version" to mod_version,
                "mc_version" to mc_version)
    }

    // copy everything else except the mcmod.info
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
