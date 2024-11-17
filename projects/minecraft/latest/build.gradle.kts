@file:Suppress("PropertyName")

val mod_id: String by project
val minecraft_version: String by project

plugins {
    id("com.github.ben-manes.versions") apply false
}

subprojects {
    apply(plugin = "com.github.ben-manes.versions")
    apply(plugin = "java")

    configure<BasePluginExtension> {
        archivesName.set("${mod_id}-${minecraft_version}")
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(17)
    }

    configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(17))
        withSourcesJar()
    }
}
