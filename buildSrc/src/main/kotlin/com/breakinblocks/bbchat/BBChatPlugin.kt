package com.breakinblocks.bbchat

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create

class BBChatPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val bbchat = project.extensions.create<BBChatPluginExtension>("bbchat")
        bbchat.mutexesDir.set(project.rootProject.buildDir.toPath().resolve("mutexes").toAbsolutePath().toString())
    }
}
