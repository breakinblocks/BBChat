pluginManagement {
    resolutionStrategy {
        eachPlugin {
            when (requested.id.toString()) {
                "com.github.ben-manes.versions" -> {
                    useModule("com.github.ben-manes:gradle-versions-plugin:0.39.0")
                }
                "com.github.johnrengelman.shadow" -> {
                    useModule("gradle.plugin.com.github.johnrengelman:shadow:7.1.1")
                }
                "net.kyori.blossom" -> {
                    useModule("net.kyori:blossom:1.2.0")
                }
                "net.minecraftforge.gradle" -> {
                    useModule("net.minecraftforge.gradle:ForgeGradle:5.1.26")
                }
                "forge" -> {
                    useModule("com.anatawa12.forge:ForgeGradle:1.2-1.0.7")
                }
                "org.parchmentmc.librarian.forgegradle" -> {
                    useModule("org.parchmentmc.librarian.forgegradle:org.parchmentmc.librarian.forgegradle.gradle.plugin:1.1.4.0-dev-SNAPSHOT")
                }
            }
        }
    }
    repositories {
        maven {
            url = uri("https://maven.minecraftforge.net")
            content {
                includeGroup("de.oceanlabs.mcp")
                includeGroup("net.minecraft")
                includeGroup("net.minecraftforge")
                includeGroup("net.minecraftforge.gradle")
            }
        }
        maven {
            url = uri("https://maven.parchmentmc.org")
            content {
                includeGroup("org.parchmentmc")
                includeGroup("org.parchmentmc.feather")
                includeGroup("org.parchmentmc.librarian.forgegradle")
            }
        }
        gradlePluginPortal {
            // Gradle plugin portal includes jcenter, migrate away from this if possible
            content {
                includeGroup("com.github.ben-manes")
                includeGroup("com.gradle.publish")
                includeGroup("gradle.plugin.com.github.johnrengelman")
                includeGroup("net.kyori")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "bbchat"

include("bbchat-common")
include("bbchat-forge")
include("bbchat-forge-1.15.2")
include("bbchat-forge-1.14.4")
include("bbchat-forge-1.12.2")
include("bbchat-forge-1.7.10")
