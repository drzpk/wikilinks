package dev.drzepka.wikilinks.generator

import dev.drzepka.wikilinks.common.config.BaseConfiguration

@Suppress("SameParameterValue")
object Configuration : BaseConfiguration() {
    val skipDeletingDumps = getBoolean("SKIP_DELETING_DUMPS", false)
    val workingDirectory = getString("WORKING_DIRECTORY", "dumps")!!
    val outputLocation = getString("OUTPUT_LOCATION", required = true)!!
    val currentVersionLocation = getString("CURRENT_VERSION_LOCATION")
    val batchMode = getBoolean("BATCH_MODE", false)

    // Debugging/test variables
    val readBlocksLimit = getInt("READ_BLOCKS_LIMIT")
}
