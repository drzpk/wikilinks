package dev.drzepka.wikilinks.generator

@Suppress("SameParameterValue")
object Configuration {
    val dumpSource = getString("DUMP_SOURCE", "https://dumps.wikimedia.org/enwiki")!!
    val skipDownloadingDumps = getBoolean("SKIP_DOWNLOADING_DUMPS", false)

    private fun getBoolean(name: String, default: Boolean): Boolean = getString(name) != null || default

    private fun getString(name: String, default: String? = null, required: Boolean = false): String? {
        val value = System.getenv(name) ?: default
        if (value == null && required)
            throw IllegalStateException("Environment variable $name wasn't set")
        return value
    }
}
