pluginManagement {
    resolutionStrategy {
        eachPlugin {
            when (requested.id.toString()) {
                "com.github.ben-manes.versions" -> {
                    useModule("com.github.ben-manes:gradle-versions-plugin:0.51.0")
                }

                "com.github.johnrengelman.shadow" -> {
                    useModule("com.github.johnrengelman:shadow:8.1.1")
                }

                "org.gradle.toolchains.foojay-resolver-convention" -> {
                    useModule("org.gradle.toolchains:foojay-resolver:0.5.0")
                }

                "com.gtnewhorizons.retrofuturagradle" -> {
                    // GTNH's ForgeGradle replacement for 1.7.10. 1.4.x is the last line supporting Gradle 8.
                    // https://nexus.gtnewhorizons.com/repository/public/com/gtnewhorizons/retrofuturagradle/maven-metadata.xml
                    useModule("com.gtnewhorizons:retrofuturagradle:1.4.9")
                }
            }
        }
    }
    repositories {
        maven {
            name = "GTNH"
            url = uri("https://nexus.gtnewhorizons.com/repository/public/")
            content {
                includeGroupByRegex("""^com\.gtnewhorizons(?:\..+$|$)""")
            }
        }
        gradlePluginPortal {
            content {
                includeGroup("com.github.ben-manes")
                includeGroup("com.github.johnrengelman")
                includeGroup("org.gradle.toolchains")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention")
}

rootProject.name = "bblink"

include("projects:core")

// GT New Horizons (Minecraft 1.7.10, Forge 10.13.4.1614)
include("projects:minecraft:v1.7.10:forge")
