pluginManagement {
    resolutionStrategy {
        eachPlugin {
            when (requested.id.toString()) {
                "com.github.ben-manes.versions" -> {
                    useModule("com.github.ben-manes:gradle-versions-plugin:0.46.0")
                }

                "com.github.johnrengelman.shadow" -> {
                    useModule("com.github.johnrengelman:shadow:8.1.1")
                }

                "net.kyori.blossom" -> {
                    useModule("net.kyori:blossom:1.3.1")
                }

                "org.spongepowered.gradle.vanilla" -> {
                    useModule("org.spongepowered.gradle.vanilla:org.spongepowered.gradle.vanilla.gradle.plugin:0.2.1-SNAPSHOT")
                }

                "org.quiltmc.loom" -> {
                    useModule("org.quiltmc:loom:1.2.3")
                }

                "net.minecraftforge.gradle" -> {
                    useModule("net.minecraftforge.gradle:ForgeGradle:6.0.11")
                }

                "forge" -> {
                    useModule("com.anatawa12.forge:ForgeGradle:1.2-1.1.0")
                }

                "org.parchmentmc.librarian.forgegradle" -> {
                    useModule("org.parchmentmc.librarian.forgegradle:org.parchmentmc.librarian.forgegradle.gradle.plugin:1.2.0")
                }

                "fabric-loom" -> {
                    useModule("fabric-loom:fabric-loom.gradle.plugin:1.2.7")
                }

                "io.github.juuxel.loom-quiltflower" -> {
                    useModule("io.github.juuxel:loom-quiltflower:1.8.0")
                }
            }
        }
    }
    repositories {
        maven {
            name = "Sponge"
            url = uri("https://repo.spongepowered.org/repository/maven-public")
            content {
                includeGroupByRegex("""^org\.spongepowered(?:\..+$|$)""")
            }
        }
        maven {
            name = "Quilt"
            url = uri("https://maven.quiltmc.org/repository/release")
            content {
                includeGroupByRegex("""^org\.quiltmc(?:\..+$|$)""")
            }
        }
        maven {
            name = "MinecraftForge"
            url = uri("https://maven.minecraftforge.net")
            content {
                includeGroup("de.oceanlabs.mcp")
                includeGroup("net.minecraft")
                includeGroupByRegex("""^net\.minecraftforge(?:\..+$|$)""")
            }
        }
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net")
            content {
                includeGroup("fabric-loom")
                includeGroupByRegex("""^net\.fabricmc(?:\..+$|$)""")
            }
        }
        maven {
            name = "Parchment"
            url = uri("https://maven.parchmentmc.org")
            content {
                includeGroupByRegex("""^org\.parchmentmc(?:\..+$|$)""")
            }
        }
        gradlePluginPortal {
            // Gradle plugin portal includes jcenter, migrate away from this if possible
            content {
                includeGroup("com.github.ben-manes")
                includeGroup("com.github.johnrengelman")
                includeGroup("com.gradle.publish")
                includeGroup("io.github.juuxel")
                includeGroup("gradle.plugin.org.jetbrains.gradle.plugin.idea-ext")
                includeGroup("net.kyori")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "bbchat"

include("projects:core")

include("projects:minecraft:latest:vanilla")
include("projects:minecraft:latest:quilt")
include("projects:minecraft:latest:forge")
include("projects:minecraft:latest:fabric")

include("projects:minecraft:v1.18.2:vanilla")
include("projects:minecraft:v1.18.2:quilt")
include("projects:minecraft:v1.18.2:forge")
include("projects:minecraft:v1.18.2:fabric")

include("projects:minecraft:v1.17.1:forge")

include("projects:minecraft:v1.16.5:forge")

include("projects:minecraft:v1.15.2:forge")

include("projects:minecraft:v1.14.4:forge")

include("projects:minecraft:v1.12.2:forge")

include("projects:minecraft:v1.7.10:forge")
