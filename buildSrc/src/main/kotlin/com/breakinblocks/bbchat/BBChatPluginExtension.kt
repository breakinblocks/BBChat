package com.breakinblocks.bbchat

import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider

abstract class BBChatPluginExtension {
    abstract val mutexesDirectory: DirectoryProperty

    fun getMutexDir(vararg dirNames: String): Provider<Directory> {
        return mutexesDirectory.map {
            var mutexDirectory = it
            for (dirName in dirNames) {
                mutexDirectory = mutexDirectory.dir(dirName)
            }

            mutexDirectory.asFile.mkdirs()
            mutexDirectory
        }
    }
}
