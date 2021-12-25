@file:Suppress("PropertyName")

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.kyori.blossom.BlossomExtension
import net.minecraftforge.gradle.delayed.DelayedFile
import net.minecraftforge.gradle.delayed.DelayedString
import net.minecraftforge.gradle.tasks.ProcessJarTask
import net.minecraftforge.gradle.tasks.user.SourceCopyTask
import net.minecraftforge.gradle.tasks.user.reobf.ArtifactSpec
import net.minecraftforge.gradle.tasks.user.reobf.ReobfTask
import net.minecraftforge.gradle.user.UserConstants
import net.minecraftforge.gradle.user.UserExtension
import net.minecraftforge.gradle.user.patch.ForgeUserPlugin

val mod_version: String by project
val mc_version: String by project
val mc_version_range_supported: String by project
val forge_version: String by project
val forge_version_range_supported: String by project

plugins {
    id("com.github.ben-manes.versions")
    id("com.github.johnrengelman.shadow")
    id("net.kyori.blossom")
    id("forge")
}

val fg_plugin = plugins.getPlugin(ForgeUserPlugin::class.java)

base.archivesName.set("bbchat-${mc_version}")

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

configure<UserExtension> {
    version = "${mc_version}-${forge_version}-${mc_version}"
    runDir = "run"
}

dependencies {
    implementation(project(path = ":bbchat-common", configuration = "shadow"))
}

// Use Blossom instead of FG source replacement
tasks.filterIsInstance(SourceCopyTask::class.java).forEach { it.enabled = false }

configure<BlossomExtension> {
    replaceToken("version = \"\"", "version = \"${mod_version}\"")
    replaceToken("dependencies = \"\"", "dependencies = \"required-after:Forge@${forge_version_range_supported};\"")
    replaceToken("acceptedMinecraftVersions = \"\"", "acceptedMinecraftVersions = \"${mc_version_range_supported}\"")
    replaceTokenIn("/BBChat.java")
}

tasks.named<ProcessResources>("processResources") {
    inputs.property("mod_version", mod_version)
    inputs.property("mc_version", mc_version)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    from(sourceSets["main"].resources.srcDirs) {
        include("mcmod.info")
        expand(
            "mod_version" to mod_version,
            "mc_version" to mc_version
        )
    }
    from(sourceSets["main"].resources.srcDirs) {
        exclude("mcmod.info")
    }
}

tasks.named<Jar>("jar") {
    manifest {
        attributes("FMLAT" to "bbchat_at.cfg")
    }
}

val shadowJar = tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("forge")
    dependencies {
        include(project(":bbchat-common"))
    }
    exclude("dummyThing")
    exclude(".cache")
    exclude("GradleStart*.class")
    exclude("net/minecraftforge/gradle/*")
}

// Source: https://github.com/Team-Fruit/BnnWidget/blob/32736398f19f7ae89874c47535f8816f02b6c2db/build.subprojects.gradle#L284-L305
// License: MIT @ https://github.com/Team-Fruit/BnnWidget/blob/32736398f19f7ae89874c47535f8816f02b6c2db/LICENSE
// Ported to Kotlin DSL by BlueAgent
val reobfShadowJar = tasks.create("reobfShadowJar", ReobfTask::class) {
    dependsOn("genSrgs")
    fun delayedString(path: String): DelayedString {
        return DelayedString(project, path, fg_plugin)
    }

    fun delayedFile(path: String): DelayedFile {
        return DelayedFile(project, path, fg_plugin)
    }
    setExceptorCfg(delayedFile(UserConstants.EXC_SRG))
    setSrg(delayedFile(UserConstants.REOBF_SRG))
    setFieldCsv(delayedFile(UserConstants.FIELD_CSV))
    setMethodCsv(delayedFile(UserConstants.METHOD_CSV))
    setMcVersion(delayedString("{MC_VERSION}"))
    mustRunAfter("test")
    mustRunAfter("shadowJar")
    reobf(shadowJar.get(), closureOf<ArtifactSpec> {
        val javaExt = project.extensions.getByType(JavaPluginExtension::class)
        classpath = javaExt.sourceSets.getByName("main").compileClasspath
    })
    extraSrg = fg_plugin.extension.srgExtra
    fun delayedDirtyFile(name: String?, classifier: String?, ext: String?, usesMappings: Boolean): DelayedFile? {
        return object : DelayedFile(project, "", fg_plugin) {
            val DIRTY_DIR = "{BUILD_DIR}/dirtyArtifacts"
            fun isNullOrEmpty(str: String?): Boolean {
                return str == null || str.isEmpty()
            }

            fun hasApiVersion(): Boolean = true
            override fun resolveDelayed(): File? {
                val decompDeobf = project.tasks.getByName("deobfuscateJar") as ProcessJarTask
                pattern =
                    (if (decompDeobf.isClean) "{API_CACHE_DIR}/" + (if (usesMappings) UserConstants.MAPPING_APPENDAGE else "") else DIRTY_DIR) + "/"
                pattern += if (!isNullOrEmpty(name)) name else "{API_NAME}"
                pattern += "-" + if (hasApiVersion()) "{API_VERSION}" else "{MC_VERSION}"
                if (!isNullOrEmpty(classifier)) pattern += "-$classifier"
                if (!isNullOrEmpty(ext)) pattern += ".$ext"
                return super.resolveDelayed()
            }
        }
    }
    afterEvaluate {
        if (fg_plugin.extension.isDecomp) {
            setDeobfFile(tasks.named<ProcessJarTask>("deobfuscateJar").get().delayedOutput)
            val srcDepName = fg_plugin.apiName + "Src"
            setRecompFile(delayedDirtyFile(srcDepName, null, "jar", true))
        }
    }
}

tasks.named<ReobfTask>("reobf") {
    dependsOn(reobfShadowJar)
}
