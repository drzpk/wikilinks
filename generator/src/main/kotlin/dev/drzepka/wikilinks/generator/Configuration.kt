package dev.drzepka.wikilinks.generator

import dev.drzepka.wikilinks.app.config.BaseConfiguration

@Suppress("SameParameterValue")
object Configuration : BaseConfiguration() {
    val skipDownloadingDumps = getBoolean("SKIP_DOWNLOADING_DUMPS", false)
    val workingDirectory = getString("WORKING_DIRECTORY", "dumps")!!
    val databasesDirectory = dev.drzepka.wikilinks.app.config.Configuration.databasesDirectory
    val batchMode = getBoolean("BATCH_MODE", false)
}
