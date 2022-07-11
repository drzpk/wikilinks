package dev.drzepka.wikilinks.app.config

abstract class BaseConfiguration {
    protected fun getBoolean(name: String, default: Boolean): Boolean = getString(name) != null || default

    protected fun getString(name: String, default: String? = null, required: Boolean = false): String? {
        val value = getEnvironmentVariable(name) ?: default
        if (value == null && required)
            throw IllegalStateException("Environment variable $name wasn't set")
        return value
    }
}
