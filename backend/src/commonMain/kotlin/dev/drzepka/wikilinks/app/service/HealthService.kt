package dev.drzepka.wikilinks.app.service

import dev.drzepka.wikilinks.app.db.ConfigRepository
import dev.drzepka.wikilinks.app.model.Health
import dev.drzepka.wikilinks.common.BuildConfig
import kotlinx.datetime.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class HealthService(private val configRepository: ConfigRepository) {
    private val startTime = Clock.System.now()

    fun getHealth(): Health {
        val msg = getErrorMessage()
        val uptimeDuration = Clock.System.now() - startTime

        return Health(
            msg == null,
            msg,
            BuildConfig.VERSION,
            configRepository.getDumpVersion(),
            uptimeDuration.inWholeSeconds.toInt()
        )
    }

    private fun getErrorMessage(): String? {
        val errors = mutableListOf<String>()
        if (configRepository.isMaintenanceModeActive())
            errors.add("Wikipedia database update in progress")
        else if (configRepository.getDumpVersion() == null)
            errors.add("Wikipedia dump version is unknown")

        return if (errors.isNotEmpty())
            errors.joinToString(separator = "; ")
        else null
    }
}
