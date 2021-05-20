pluginManagement {
    resolutionStrategy {
        eachPlugin {
            when (requested.id.toString()) {
                "com.github.johnrengelman.shadow" -> {
                    useModule("gradle.plugin.com.github.jengelman.gradle.plugins:shadow:7.0.0")
                }
                "net.kyori.blossom" -> {
                    useModule("net.kyori:blossom:1.2.0")
                }
                "net.minecraftforge.gradle" -> {
                    // TODO: Change when FG5 actually works
                    //useModule("net.minecraftforge.gradle:ForgeGradle:5.0.0")
                    useModule("tk.sciwhiz12.gradle:ForgeGradle:4.1.9")
                }
                "forge" -> {
                    useModule("com.anatawa12.forge:ForgeGradle:1.2-1.0.6")
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
            name = "sciwhiz12"
            url = uri("https://sciwhiz12.tk/maven")
            content {
                includeGroup("tk.sciwhiz12.gradle")
            }
        }
        gradlePluginPortal {
            // Gradle plugin portal includes jcenter, migrate away from this if possible
            content {
                includeGroup("com.gradle.publish")
                includeGroup("gradle.plugin.com.github.jengelman.gradle.plugins")
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
