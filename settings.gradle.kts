pluginManagement {
    resolutionStrategy {
        eachPlugin {
            when {
                requested.id.toString() == "com.github.johnrengelman.shadow" -> {
                    useModule("com.github.jengelman.gradle.plugins:shadow:5.2.0")
                }
                requested.id.toString() == "net.minecraftforge.gradle" -> {
                    useModule("net.minecraftforge.gradle:ForgeGradle:3.0.190")
                }
                requested.id.toString() == "net.minecraftforge.gradle.forge" -> {
                    useModule("com.anatawa12.forge:ForgeGradle:2.3-1.0.2")
                }
                requested.id.toString() == "forge" -> {
                    useModule("com.anatawa12.forge:ForgeGradle:1.2-1.0.4")
                }
            }
        }
    }
    repositories {
        maven {
            url = uri("https://files.minecraftforge.net/maven")
            content {
                includeGroup("net.minecraftforge")
                includeGroup("net.minecraftforge.gradle")
            }
        }
        mavenCentral {
            content {
                includeGroup("com.anatawa12.forge")
            }
        }
        gradlePluginPortal {
            content {
                includeGroup("com.github.jengelman.gradle.plugins")
            }
        }
    }
}

rootProject.name = "bbchat"

include("bbchat-common")
include("bbchat-forge")
include("bbchat-forge-1.15.2")
include("bbchat-forge-1.14.4")
include("bbchat-forge-1.12.2")
include("bbchat-forge-1.7.10")
