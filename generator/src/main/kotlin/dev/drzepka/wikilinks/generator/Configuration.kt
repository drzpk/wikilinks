package dev.drzepka.wikilinks.generator

import dev.drzepka.wikilinks.app.config.BaseConfiguration
import kotlin.time.Duration.Companion.seconds

@Suppress("SameParameterValue")
object Configuration : BaseConfiguration() {
    val skipDeletingDumps = getBoolean("SKIP_DELETING_DUMPS", false)
    val workingDirectory = getString("WORKING_DIRECTORY", "dumps")!!
    val databasesDirectory = dev.drzepka.wikilinks.app.config.Configuration.databasesDirectory
    val batchMode = getBoolean("BATCH_MODE", false)


    /**
     * Maximum amount of time to wait before closing database connection after detecting maintenance mode.
     * This allows most of the database queries to complete before the server becomes unavailable.
     **/
    val databaseDisconnectTimeout = 30.seconds
}
