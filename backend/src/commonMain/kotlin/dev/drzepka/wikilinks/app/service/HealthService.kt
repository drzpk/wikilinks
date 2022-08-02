package dev.drzepka.wikilinks.app.service

import dev.drzepka.wikilinks.app.model.Health
import dev.drzepka.wikilinks.common.BuildConfig
import kotlinx.datetime.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class HealthService(private val dumpUpdaterService: DumpUpdaterService) {
    private val startTime = Clock.System.now()

    fun getHealth(): Health {
        val msg = getErrorMessage()
        val uptimeDuration = Clock.System.now() - startTime

        return Health(
            msg == null,
            msg,
            BuildConfig.VERSION,
            dumpUpdaterService.dumpVersion,
            uptimeDuration.inWholeSeconds.toInt()
        )
    }

    private fun getErrorMessage(): String? {
        val errors = mutableListOf<String>()
        if (dumpUpdaterService.isUpdateInProgress())
            errors.add("Wikipedia database update in progress")

        return if (errors.isNotEmpty())
            errors.joinToString(separator = "; ")
        else null
    }
}
