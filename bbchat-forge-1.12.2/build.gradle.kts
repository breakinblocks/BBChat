buildscript {
    repositories {
        mavenCentral()
        maven { url = 'https://files.minecraftforge.net/maven' }
    }
    dependencies {
        classpath('com.anatawa12.forge:ForgeGradle:2.3-1.0.+') {
            changing = true
        }
    }
}

apply plugin: 'net.minecraftforge.gradle.forge'
apply plugin: 'com.github.johnrengelman.shadow'

archivesBaseName = "bbchat-${mc_version}"

minecraft {
    version = "${mc_version}-${forge_version}"
    runDir = 'run'
    mappings = "${mappings_version}"

    replace 'version = ""', "version = \"${mod_version}\""
    replace 'dependencies = ""', "dependencies = \"required-after:forge@${forge_version_range_supported};\""
    replace 'acceptedMinecraftVersions = ""', "acceptedMinecraftVersions = \"${mc_version_range_supported}\""
    replaceIn "BBChat.java"
}

dependencies {
    compile project(path: ':bbchat-common', configuration: 'shadow')
}

processResources {
    // this will ensure that this task is redone when the versions change.
    inputs.property 'mod_version', project.mod_version
    inputs.property 'mc_version', project.mc_version

    // replace stuff in mcmod.info, nothing else
    from(sourceSets.main.resources.srcDirs) {
        include 'mcmod.info'

        // replace mod_version and mc_version_range_supported and forge_version_major
        expand 'mod_version': "${mod_version}",
                'mc_version': "${mc_version}"
    }

    // copy everything else except the mcmod.info
    from(sourceSets.main.resources.srcDirs) {
        exclude 'mcmod.info'
    }
}

shadowJar {
    classifier = 'forge'
    dependencies {
        include(project(':bbchat-common'))
    }
    exclude 'dummyThing'
}

reobf {
    shadowJar {}
}

build.dependsOn(shadowJar)
