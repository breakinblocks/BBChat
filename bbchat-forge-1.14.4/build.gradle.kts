buildscript {
    repositories {
        maven { url = 'https://files.minecraftforge.net/maven' }
    }
    dependencies {
        classpath group: 'net.minecraftforge.gradle', name: 'ForgeGradle', version: '3.+', changing: true
    }
}

apply plugin: 'net.minecraftforge.gradle'
apply plugin: 'com.github.johnrengelman.shadow'

archivesBaseName = "bbchat-${mc_version}"

minecraft {
    mappings channel: 'snapshot', version: "${mappings_version}"

    runs {
        client {
            workingDirectory project.file('run')

            // Recommended logging data for a userdev environment
            property 'forge.logging.markers', 'SCAN,REGISTRIES,REGISTRYDUMP'

            // Recommended logging level for the console
            property 'forge.logging.console.level', 'debug'

            mods {
                bbchat {
                    source sourceSets.main
                }
            }
        }

        server {
            workingDirectory project.file('run')

            // Recommended logging data for a userdev environment
            property 'forge.logging.markers', 'SCAN,REGISTRIES,REGISTRYDUMP'

            // Recommended logging level for the console
            property 'forge.logging.console.level', 'debug'

            mods {
                bbchat {
                    source sourceSets.main
                }
            }
        }

        data {
            workingDirectory project.file('run')

            // Recommended logging data for a userdev environment
            property 'forge.logging.markers', 'SCAN,REGISTRIES,REGISTRYDUMP'

            // Recommended logging level for the console
            property 'forge.logging.console.level', 'debug'

            args '--mod', 'bbchat', '--all', '--output', file('src/generated/resources/')

            mods {
                bbchat {
                    source sourceSets.main
                }
            }
        }
    }
}

dependencies {
    minecraft "net.minecraftforge:forge:${mc_version}-${forge_version}"
    compile project(path: ':bbchat-common', configuration: 'shadow')
}

processResources {
    // this will ensure that this task is redone when the versions change.
    inputs.property 'mod_version', project.mod_version
    inputs.property 'mc_version_range_supported', project.mc_version_range_supported
    inputs.property 'forge_version_range_supported', project.forge_version_range_supported

    // replace stuff in mods.toml, nothing else
    from(sourceSets.main.resources.srcDirs) {
        include 'META-INF/mods.toml'

        // replace mod_version and mc_version_range_supported and forge_version_major
        expand 'mod_version': "${mod_version}",
                'mc_version_range_supported': "${mc_version_range_supported}",
                'forge_version_range_supported': "${forge_version_range_supported}"
    }

    // copy everything else except the mods.toml
    from(sourceSets.main.resources.srcDirs) {
        exclude 'META-INF/mods.toml'
    }
}

// Example for how to get properties into the manifest for reading by the runtime..
jar {
    manifest {
        attributes([
                "Specification-Title"     : "BBChat",
                "Specification-Vendor"    : "Breakin' Blocks",
                "Specification-Version"   : "1", // We are version 1 of ourselves
                "Implementation-Title"    : project.name,
                "Implementation-Version"  : "${version}",
                "Implementation-Vendor"   : "Breakin' Blocks",
                "Implementation-Timestamp": new Date().format("yyyy-MM-dd'T'HH:mm:ssZ")
        ])
    }
}

shadowJar {
    classifier = "forge"
    dependencies {
        include(project(':bbchat-common'))
    }
}

reobf {
    shadowJar {}
}

build.dependsOn(shadowJar)
