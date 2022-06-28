package dev.drzepka.wikilinks.generator

import dev.drzepka.wikilinks.app.config.BaseConfiguration

@Suppress("SameParameterValue")
object Configuration : BaseConfiguration() {
    val dumpSource = getString("DUMP_SOURCE", "https://dumps.wikimedia.org/enwiki")!!
    val skipDownloadingDumps = getBoolean("SKIP_DOWNLOADING_DUMPS", false)
    val workingDirectory = getString("WORKING_DIRECTORY", "dumps")!!
    val databasesDirectory = dev.drzepka.wikilinks.app.config.Configuration.databasesDirectory
}
