@file:Suppress("PropertyName")

val mod_version: String by project

plugins {
    id("bbchat") apply false
}

allprojects {
    apply(plugin = "bbchat")

    group = "com.breakinblocks.bbchat"
    version = mod_version
}

subprojects {
    repositories {
        exclusiveContent {
            forRepository {
                maven {
                    name = "Quilt"
                    url = uri("https://maven.quiltmc.org/repository/release")
                }
            }
            filter {
                includeGroupByRegex("""^org\.quiltmc(?:\..+$|$)""")
            }
        }
        exclusiveContent {
            forRepository {
                maven {
                    name = "Parchment"
                    url = uri("https://maven.parchmentmc.org")
                }
            }
            filter {
                includeGroupByRegex("""^org\.parchmentmc(?:\..+$|$)""")
            }
        }
        exclusiveContent {
            forRepository {
                maven {
                    name = "Fuzs Mod Resources"
                    url = uri("https://raw.githubusercontent.com/Fuzss/modresources/main/maven")
                }
            }
            filter {
                includeGroupByRegex("""^fuzs(?:\..+$|$)""")
                includeModule("net.minecraftforge", "forgeconfigapiport-fabric")
            }
        }
        mavenCentral {
            content {
                includeGroup("com.fasterxml")
                includeGroup("com.fasterxml.jackson")
                includeGroup("com.fasterxml.jackson.core")
                includeGroup("com.google.code.findbugs")
                includeGroup("com.google.guava")
                includeGroup("com.neovisionaries")
                includeGroup("com.squareup.okhttp3")
                includeGroup("com.squareup.okio")
                includeGroup("net.dv8tion")
                includeGroup("net.sf.trove4j")
                includeGroup("org.apache")
                includeGroup("org.apache.commons")
                includeGroup("org.apache.logging")
                includeGroup("org.apache.logging.log4j")
                includeGroup("org.jetbrains")
                includeGroup("org.jetbrains.kotlin")
                includeGroup("org.slf4j")
                includeGroup("org.sonatype.oss")
            }
        }
    }
}
