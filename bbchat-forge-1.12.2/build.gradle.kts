@file:Suppress("PropertyName")

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.minecraftforge.gradle.user.IReobfuscator
import net.minecraftforge.gradle.user.patcherUser.forge.ForgeExtension

val mod_version: String by project
val mc_version: String by project
val mc_version_range_supported: String by project
val forge_version: String by project
val forge_version_range_supported: String by project
val mappings_version: String by project

buildscript {
    repositories {
        maven { url = uri("https://files.minecraftforge.net/maven") }
    }
    dependencies {
        classpath(group = "com.anatawa12.forge", name = "ForgeGradle", version = "2.3-1.0.2") { isChanging = true }
    }
}

apply(plugin = "net.minecraftforge.gradle.forge")
apply(plugin = "com.github.johnrengelman.shadow")

base.archivesBaseName = "bbchat-${mc_version}"

configure<ForgeExtension> {
    version = "${mc_version}-${forge_version}"
    runDir = "run"
    mappings = "${mappings_version}"

    replace("version = \"\"", "version = \"${mod_version}\"")
    replace("dependencies = \"\"", "dependencies = \"required-after:forge@${forge_version_range_supported};\"")
    replace("acceptedMinecraftVersions = \"\"", "acceptedMinecraftVersions = \"${mc_version_range_supported}\"")
    replaceIn("BBChat.java")
}

dependencies {
    implementation(project(path = ":bbchat-common", configuration = "shadow"))
}

tasks.named<ProcessResources>("processResources") {
    // this will ensure that this task is redone when the versions change.
    inputs.property("mod_version", mod_version)
    inputs.property("mc_version", mc_version)

    // replace stuff in mcmod.info, nothing else
    from(sourceSets["main"].resources.srcDirs) {
        include("mcmod.info")

        // replace mod_version and mc_version_range_supported and forge_version_major
        expand("mod_version" to "${mod_version}",
                "mc_version" to "${mc_version}")
    }

    // copy everything else except the mcmod.info
    from(sourceSets["main"].resources.srcDirs) {
        exclude("mcmod.info")
    }
}

val shadowJar = tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("forge")
    dependencies {
        include(project(":bbchat-common"))
    }
    exclude("dummyThing")
}

extensions.configure<NamedDomainObjectContainer<IReobfuscator>> {
    create("shadowJar")
}

tasks.named<DefaultTask>("build") {
    dependsOn(shadowJar)
}
