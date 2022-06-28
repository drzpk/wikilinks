package dev.drzepka.wikilinks.app.db

import dev.drzepka.wikilinks.common.utils.MultiplatformFile
import mu.KotlinLogging

class FileConfigRepository(workingDirectory: String) : ConfigRepository {
    private val log = KotlinLogging.logger {}

    private val versionFile = MultiplatformFile("$workingDirectory/dump_version.txt")
    private val maintenanceModeFile = MultiplatformFile("$workingDirectory/maintenance_mode")

    override fun getDumpVersion(): String {
        val version = versionFile.read().trim()
        log.info { "Current dump version: $version" }
        return version
    }

    override fun setDumpVersion(version: String) {
        log.info { "Writing dump version: $version" }
        versionFile.write(version)
    }

    override fun isMaintenanceModeActive(): Boolean {
        val exists = maintenanceModeFile.isFile()
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
}