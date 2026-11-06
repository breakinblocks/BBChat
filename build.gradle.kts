@file:Suppress("PropertyName")

val mod_version: String by project
val mod_group_id: String by project

allprojects {
    group = mod_group_id
    version = mod_version
}

subprojects {
    repositories {
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
                includeGroupByRegex("""^org\.slf4j(?:\..+$|$)""")
                includeGroupByRegex("""^org\.xerial(?:\..+$|$)""")
                // Tests
                includeGroupByRegex("""^org\.junit(?:\..+$|$)""")
                includeGroupByRegex("""^org\.opentest4j(?:\..+$|$)""")
                includeGroupByRegex("""^org\.apiguardian(?:\..+$|$)""")
            }
        }
    }
}
