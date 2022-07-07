package dev.drzepka.wikilinks.app.db

interface ConfigRepository {
    fun getDumpVersion(): String?
    fun setDumpVersion(version: String)
    fun isMaintenanceModeActive(): Boolean
    fun setMaintenanceMode(state: Boolean)
    fun isGeneratorActive(): Boolean
    fun setGeneratorActive(state: Boolean)
}
