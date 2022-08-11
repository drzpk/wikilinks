package dev.drzepka.wikilinks.generator

import dev.drzepka.wikilinks.common.config.BaseConfiguration

@Suppress("SameParameterValue")
object Configuration : BaseConfiguration() {
    val skipDeletingDumps = getBoolean("SKIP_DELETING_DUMPS", false)
    val workingDirectory = getString("WORKING_DIRECTORY", "dumps")!!
    val outputLocation = getString("OUTPUT_LOCATION", required = true)!!
    val batchMode = getBoolean("BATCH_MODE", false)
}
