package dev.drzepka.wikilinks.app.db

import dev.drzepka.wikilinks.common.utils.MultiplatformFile
import mu.KotlinLogging

class FileConfigRepository(workingDirectory: String) : ConfigRepository {
    private val log = KotlinLogging.logger {}

    private val generatorActiveFile = MultiplatformFile("$workingDirectory/$GENERATOR_ACTIVE_FILE_NAME")

    override fun isGeneratorActive(): Boolean {
        val exists = generatorActiveFile.isFile()
        log.info {
            val status = if (exists) "active" else "inactive"
            "Generator status: $status"
        }
        return exists
    }

    override fun setGeneratorActive(state: Boolean) {
        log.info {
            val desc = if (state) "active" else "inactive"
            "Setting generator status: $desc"
        }
        if (state)
            generatorActiveFile.create()
        else
            generatorActiveFile.delete()
    }

    companion object {
        private const val GENERATOR_ACTIVE_FILE_NAME = "generator_active"
    }
}
