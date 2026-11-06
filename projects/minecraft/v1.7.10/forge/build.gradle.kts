@file:Suppress("PropertyName")

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.gtnewhorizons.retrofuturagradle.mcp.ReobfuscatedJar
import com.gtnewhorizons.retrofuturagradle.modutils.ModUtils

// Built with RetroFuturaGradle, the toolchain GT New Horizons uses for 1.7.10 mods.

val mod_version: String by project
val mc_version: String by project
val slf4j_version: String by project

plugins {
    id("com.github.ben-manes.versions")
    id("com.github.johnrengelman.shadow")
    id("com.gtnewhorizons.retrofuturagradle")
}

base.archivesName.set("bblink-${mc_version}")

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
        vendor.set(JvmVendorSpec.AZUL)
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

minecraft {
    mcVersion.set(mc_version)
    username.set("Developer")
    injectedTags.put("VERSION", mod_version)
    // The shaded relay libraries never touch Minecraft classes, so there's nothing to remap in them.
    groupsToExcludeFromAutoReobfMapping.addAll("com.breakinblocks.bblink", "org.slf4j")
}

tasks.injectTags {
    outputClassName.set("com.breakinblocks.bblink.forge.Tags")
}

repositories {
    maven {
        name = "GTNH"
        url = uri("https://nexus.gtnewhorizons.com/repository/public/")
    }
}

// What goes into the release jar. Kept separate from runtimeClasspath because RFG adds a deobfuscation transform
// there, and shadowJar resolving that while the task graph is built fails when core's jar isn't built yet (clean
// builds, fresh checkouts).
val shade: Configuration by configurations.creating {
    isTransitive = false
}
// RFG requests its deobfuscation transform on every configuration (after this script's configuration blocks run);
// opt this one out once it has.
afterEvaluate {
    shade.attributes.attribute(ModUtils.DEOBFUSCATOR_TRANSFORMED, false)
}

dependencies {
    implementation(project(path = ":projects:core", configuration = "shadow"))
    // Minecraft 1.7.10 doesn't ship SLF4J, which JDA and the relay log through. Bundled and relocated below.
    implementation("org.slf4j:slf4j-api:${slf4j_version}")

    shade(project(path = ":projects:core", configuration = "shadow"))
    shade("org.slf4j:slf4j-api:${slf4j_version}")
}

tasks.processResources {
    inputs.property("mod_version", mod_version)
    inputs.property("mc_version", mc_version)
    filesMatching("mcmod.info") {
        expand("mod_version" to mod_version, "mc_version" to mc_version)
    }
}

val shadowJar = tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("dev-all")
    configurations = listOf(shade)
    // Other 1.7.10 mods may ship their own (often older) SLF4J; keep ours private.
    relocate("org.slf4j", "com.breakinblocks.bblink.shadow.org.slf4j")
    // Renames META-INF/services/org.slf4j... to match the relocation so our log4j provider is still found.
    mergeServiceFiles()
    exclude("META-INF/versions/**")
    exclude("module-info.class")
}

// Reobfuscate the shaded jar (not the plain one) so the release jar includes the relay libraries.
tasks.named<ReobfuscatedJar>("reobfJar") {
    inputJar.set(shadowJar.flatMap { it.archiveFile })
}

tasks.build {
    dependsOn(tasks.named("reobfJar"))
}
