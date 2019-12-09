/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.file.FileCollection
import java.io.File
import java.util.HashSet

internal class TaskOutputsBackup(private val outputs: FileCollection) {
    private val previousOutputs: Map<File, ByteArray>

    init {
        val outputFiles = HashSet<File>()
        outputs.forEach {
            if (it.isDirectory) {
                it.walk().filterTo(outputFiles, File::isFile)
            } else if (it.isFile) {
                outputFiles.add(it)
            }
        }

        // keysToMap usage was removed, because it breaks IC with Gradle < 5 and Gradle KTS
        previousOutputs = outputFiles.map { it to it.readBytes() }.toMap()
    }

    fun restoreOutputs() {
        outputs.forEach {
            if (it.isDirectory) {
                it.deleteRecursively()
            } else if (it.isFile) {
                it.delete()
            }
        }

        val dirs = HashSet<File>()

        for ((file, bytes) in previousOutputs) {
            val dir = file.parentFile
            if (dirs.add(dir)) {
                dir.mkdirs()
            }
            file.writeBytes(bytes)
        }
    }
}
