package dev.drzepka.wikilinks.generator

import dev.drzepka.wikilinks.common.config.BaseConfiguration
import dev.drzepka.wikilinks.common.config.CommonConfiguration

@Suppress("SameParameterValue")
object Configuration : BaseConfiguration() {
    val skipDeletingDumps = getBoolean("SKIP_DELETING_DUMPS", false)
    val workingDirectory = getString("WORKING_DIRECTORY", "dumps")!!
    val databasesDirectory = CommonConfiguration.databasesDirectory
    val batchMode = getBoolean("BATCH_MODE", false)
}
