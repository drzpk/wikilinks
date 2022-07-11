package dev.drzepka.wikilinks.app.db

import dev.drzepka.wikilinks.common.utils.MultiplatformFile
import mu.KotlinLogging

class FileConfigRepository(workingDirectory: String) : ConfigRepository {
    private val log = KotlinLogging.logger {}

    private val versionFile = MultiplatformFile("$workingDirectory/$DUMP_VERSION_FILE_NAME")
    private val maintenanceModeFile = MultiplatformFile("$workingDirectory/$MAINTENANCE_MODE_FILE_NAME")
    private val generatorActiveFile = MultiplatformFile("$workingDirectory/$GENERATOR_ACTIVE_FILE_NAME")

    private var dumpVersion: String? = null

    override fun getDumpVersion(): String? {
        if (dumpVersion != null)
            return dumpVersion

        if (!versionFile.isFile())
            return null

        dumpVersion = versionFile.read().trim()
        log.info { "Current dump version: $dumpVersion" }
        return dumpVersion
    }

    override fun setDumpVersion(version: String) {
        log.info { "Writing dump version: $version" }
        versionFile.write(version)
        dumpVersion = version
    }

    override fun isMaintenanceModeActive(): Boolean {
        val exists = maintenanceModeFile.isFile()

        if (exists)
            dumpVersion = null

        log.info {
            val status = if (exists) "active" else "inactive"
            "Maintenance mode status: $status"
        }
        return exists
    }

    override fun setMaintenanceMode(state: Boolean) {
        log.info {
            val desc = if (state) "active" else "inactive"
            "Setting maintenance mode status: $desc"
        }
        if (state)
            maintenanceModeFile.create()
        else
            maintenanceModeFile.delete()
    }

    override fun isGeneratorActive(): Boolean {
        // todo: return false if file modification time is older than generator max run time
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
        private const val DUMP_VERSION_FILE_NAME = "dump_version.txt"
        private const val MAINTENANCE_MODE_FILE_NAME = "maintenance_mode"
        private const val GENERATOR_ACTIVE_FILE_NAME = "generator_active"
    }
}
