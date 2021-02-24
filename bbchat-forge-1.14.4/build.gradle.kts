@file:Suppress("PropertyName")

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
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

        create("data") {
            workingDirectory(file("run"))

            // Recommended logging data for a userdev environment
            property("forge.logging.markers", "SCAN,REGISTRIES,REGISTRYDUMP")

            // Recommended logging level for the console
            property("forge.logging.console.level", "debug")

            setArgs(listOf("--mod", "bbchat", "--all", "--output", file("src/generated/resources/")))

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

tasks.named<ProcessResources>("processResources") {
    // this will ensure that this task is redone when the versions change.
    inputs.property("mod_version", mod_version)
    inputs.property("mc_version_range_supported", mc_version_range_supported)
    inputs.property("forge_version_range_supported", forge_version_range_supported)

    // replace stuff in mods.toml, nothing else
    from(sourceSets["main"].resources.srcDirs) {
        include("META-INF/mods.toml")

        // replace mod_version and mc_version_range_supported and forge_version_major
        expand("mod_version" to "${mod_version}",
                "mc_version_range_supported" to "${mc_version_range_supported}",
                "forge_version_range_supported" to "${forge_version_range_supported}"
        )
    }

    // copy everything else except the mods.toml
    from(sourceSets["main"].resources.srcDirs) {
        exclude("META-INF/mods.toml")
    }
}

// Example for how to get properties into the manifest for reading by the runtime..
tasks.named<Jar>("jar") {
    manifest {
        attributes(
                "Specification-Title" to "BBChat",
                "Specification-Vendor" to "Breakin' Blocks",
                "Specification-Version" to "1", // We are version 1 of ourselves
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
                "Implementation-Vendor" to "Breakin' Blocks"
                //"Implementation-Timestamp" to Date().format("yyyy-MM-dd'T'HH:mm:ssZ")
        )
    }
}

val shadowJar = tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("forge")
    dependencies {
        include(project(":bbchat-common"))
    }
}

extensions.configure<NamedDomainObjectContainer<RenameJarInPlace>> {
    create("shadowJar")
}

tasks.named<DefaultTask>("build") {
    dependsOn(shadowJar)
}
