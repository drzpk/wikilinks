package dev.drzepka.wikilinks.generator.utils

import java.io.File
import kotlin.time.Duration

class GeneratorStatus private constructor(private val generatorActiveFile: File, maxFileAge: Duration?) {
    private val maxFileAge = maxFileAge ?: Duration.INFINITE

    fun isGeneratorActive(): Boolean {
        if (generatorActiveFile.isFile && generatorActiveFile.lastModified() < System.currentTimeMillis() - maxFileAge.inWholeMilliseconds) {
            println("Generator Activation max age ($maxFileAge) exceeded")
            return false
        }

        return generatorActiveFile.isFile
    }

    fun setGeneratorActive(state: Boolean) {
        if (state) {
            if (!generatorActiveFile.createNewFile())
                generatorActiveFile.setLastModified(System.currentTimeMillis())
        } else generatorActiveFile.delete()
    }

    companion object {
        fun fromDirectory(directory: File, maxFileAge: Duration? = null): GeneratorStatus = GeneratorStatus(File(directory, "generator_active"), maxFileAge)

        fun fromFile(file: File, maxFileAge: Duration? = null): GeneratorStatus = GeneratorStatus(file, maxFileAge)
    }
}
