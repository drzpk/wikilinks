package dev.drzepka.wikilinks.updater

import java.time.Duration

// todo: use BaseConfiguration class
object Config {
    const val LOCATOR_TAG_VALUE = "This tag is used to locate EC2 instances running WikiLinks Generator."
    val GENERATOR_INSTANCE_MAX_AGE: Duration = Duration.ofHours(12)

    val databasesDirectory = getString("DATABASES_DIRECTORY", required = true)!!
    val launchTemplateId = getString("LAUNCH_TEMPLATE_ID", required = true)!!

    private fun getString(name: String, default: String? = null, required: Boolean = false): String? {
        val value = System.getProperty(name) ?: System.getenv(name) ?: default
        if (value == null && required)
            throw IllegalStateException("Environment variable $name wasn't set")
        return value
    }
}
