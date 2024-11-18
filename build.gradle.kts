@file:Suppress("PropertyName")

val mod_version: String by project
val mod_group_id: String by project

plugins {
    id("bbchat") apply false
}

allprojects {
    apply(plugin = "bbchat")

    group = mod_group_id
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
        mavenCentral {
            content {
                includeGroupByRegex("""^com\.electronwill(?:\..+$|$)""")
                includeGroupByRegex("""^com\.fasterxml(?:\..+$|$)""")
                includeGroupByRegex("""^com\.google(?:\..+$|$)""")
                includeGroupByRegex("""^com\.neovisionaries(?:\..+$|$)""")
                includeGroupByRegex("""^com\.squareup(?:\..+$|$)""")
                includeGroupByRegex("""^net\.dv8tion(?:\..+$|$)""")
                includeGroupByRegex("""^net\.sf(?:\..+$|$)""")
                includeGroupByRegex("""^org\.apache(?:\..+$|$)""")
                includeGroupByRegex("""^org\.jetbrains(?:\..+$|$)""")
                includeGroupByRegex("""^org\.junit(?:\..+$|$)""")
                includeGroupByRegex("""^org\.slf4j(?:\..+$|$)""")
                includeGroupByRegex("""^org\.sonatype(?:\..+$|$)""")
            }
        }
    }
}
