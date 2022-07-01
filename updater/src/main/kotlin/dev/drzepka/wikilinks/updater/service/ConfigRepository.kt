package dev.drzepka.wikilinks.updater.service

import dev.drzepka.wikilinks.common.WikiConfig.DUMP_VERSION_FILE_NAME
import dev.drzepka.wikilinks.common.WikiConfig.MAINTENANCE_MODE_FILE_NAME
import dev.drzepka.wikilinks.common.utils.MultiplatformFile
import dev.drzepka.wikilinks.updater.Config.databasesDirectory

// todo: FileConfigRepository should be used instead of this class
class ConfigRepository {

    private val versionFile = MultiplatformFile("$databasesDirectory/.$DUMP_VERSION_FILE_NAME")
    private val maintenanceModeFile = MultiplatformFile("$databasesDirectory/$MAINTENANCE_MODE_FILE_NAME")

    fun getCurrentVersion(): String? = if (versionFile.isFile()) versionFile.read() else null

    fun isMaintenanceModeActive(): Boolean = maintenanceModeFile.isFile()

    fun deactivateMaintenanceMode() = maintenanceModeFile.delete()
}
