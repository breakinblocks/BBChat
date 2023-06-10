package com.breakinblocks.bbchat

import org.gradle.api.provider.Property
import kotlin.io.path.Path

abstract class BBChatPluginExtension {
    abstract val mutexesDir: Property<String>

    fun getMutexDir(vararg dirNames: String): String {
        var mutexPath = Path(mutexesDir.get())
        for (dirName in dirNames) {
            mutexPath = mutexPath.resolve(dirName)
        }
        mutexPath = mutexPath.toAbsolutePath()
        mutexPath.toFile().mkdirs()
        return mutexPath.toString()
    }
}
