package dev.drzepka.wikilinks.app

import dev.drzepka.wikilinks.common.config.BaseConfiguration

object BackendConfiguration : BaseConfiguration() {
    val enableAnalytics by lazy { getBoolean("ENABLE_ANALYTICS", default = false) }
}
