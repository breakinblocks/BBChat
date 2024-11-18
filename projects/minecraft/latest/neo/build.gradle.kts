@file:Suppress("PropertyName")

import org.gradle.util.Path

val mod_id: String by project
val mod_version: String by project
val minecraft_version: String by project
val minecraft_version_range_supported: String by project
val neo_loader_version_range: String by project
val neo_version: String by project
val neo_version_range_supported: String by project
val parchment_minecraft_version: String by project
val parchment_version: String by project

plugins {
    id("com.github.ben-manes.versions")
    id("com.github.johnrengelman.shadow")
    id("net.neoforged.gradle.userdev")
}

val corePath = ":projects:core"
val parentPath = Path.path(project.path).parent!!.path!!
val vanillaPath = Path.path(parentPath).child("vanilla").path!!
evaluationDependsOn(vanillaPath)

subsystems {
    parchment {
        minecraftVersion = parchment_minecraft_version
        mappingsVersion = parchment_version
        addRepository = false
    }
}

runs {
    configureEach {
        systemProperty("forge.logging.markers", "SCAN,REGISTRIES,REGISTRYDUMP")
        systemProperty("forge.logging.console.level", "debug")
        modSource(project.sourceSets.main.get())
        modSource(project(vanillaPath).sourceSets["main"])
    }
    create("client") {
        systemProperty("forge.enabledGameTestNamespaces", mod_id)
    }
    create("server") {
        systemProperty("forge.enabledGameTestNamespaces", mod_id)
        programArgument("--nogui")
    }
    create("gameTestServer") {
        systemProperty("forge.enabledGameTestNamespaces", mod_id)
    }
    create("data") {
        programArguments.addAll(
            "--mod", "bbchat",
            "--all",
            "--output", file("src/generated/resources/").absolutePath,
            "--existing", file("src/main/resources/").absolutePath,
        )
    }
}

sourceSets.main {
    resources {
        srcDir("src/generated/resources")
    }
}

configurations {
    runtimeClasspath {
        extendsFrom(localRuntime.get())
    }
}

dependencies {
    implementation("net.neoforged:neoforge:${neo_version}")
    implementation(project(path = corePath, configuration = "shadow"))
    compileOnly(project(path = vanillaPath))
}

tasks.test {
    enabled = false
}

tasks.withType<JavaCompile> {
    source(project(vanillaPath).sourceSets.main.get().allSource)
}

tasks.processResources {
    from(project(vanillaPath).sourceSets.main.get().resources)
    val replaceProperties = mapOf(
        "mod_version" to mod_version,
        "neo_loader_version_range" to neo_loader_version_range,
        "minecraft_version_range_supported" to minecraft_version_range_supported,
        "neo_version_range_supported" to neo_version_range_supported
    )
    inputs.properties(replaceProperties)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    filesMatching(listOf("META-INF/neoforge.mods.toml")) {
        expand(replaceProperties)
    }
    from(sourceSets["main"].resources.srcDirs) {
        exclude("META-INF/mods.toml")
    }
}

tasks.jar {
    manifest {
        attributes(mapOf(
            "Specification-Title" to "BBChat",
            "Specification-Vendor" to "Breakin' Blocks",
            "Specification-Version" to "1",
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "Breakin' Blocks"
        ))
    }
}

tasks.shadowJar {
    archiveClassifier.set(project.name)
    dependencies {
        include(project(corePath))
    }
}

//extensions.configure<NamedDomainObjectContainer<RenameJarInPlace>> {
//    create("shadowJar")
//}

tasks.build {
    dependsOn(tasks.shadowJar)
}
