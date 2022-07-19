package dev.drzepka.wikilinks.app.service

import dev.drzepka.wikilinks.app.db.ConfigRepository
import dev.drzepka.wikilinks.app.db.DatabaseProvider
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import mu.KotlinLogging
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class AvailabilityService(
    scope: CoroutineScope,
    private val configRepository: ConfigRepository,
    private val databaseProvider: DatabaseProvider
) {
    private val log = KotlinLogging.logger {}
    private val updateInProgress = atomic(false)

    init {
        scope.launch {
            while (isActive) {
                checkForMaintenanceMode()
            }
        }
    }

    fun isUpdateInProgress(): Boolean = updateInProgress.value

    private suspend fun checkForMaintenanceMode() {
        // This solution is not ideal, because there may be cases when more time is required
        // to finish all queries that the hardcoded values. But it should be sufficient
        // most of the time. An edge case-free solution would probably require tracking
        // and waiting for all ongoing requests before releasing database connections.

        delay(10.seconds)
        if (!configRepository.isMaintenanceModeActive())
            return

        log.info { "Maintenance mode detected" }
        updateInProgress.value = true

        databaseProvider.closeAllConnections()
        waitForMaintenanceModeToTurnOff()

        log.info { "Maintenance mode disabled" }
        updateInProgress.value = false
    }

    private suspend fun waitForMaintenanceModeToTurnOff() {
        log.info { "Waiting for maintenance mode to turn off" }
        val warnDuration = 1.minutes
        val interruptDuration = 3.minutes

        val startTime = Clock.System.now()
        var warningPrinted = false

        while (true) {
            delay(5.seconds)
            if (!configRepository.isMaintenanceModeActive())
                break

            val duration = Clock.System.now() - startTime
            if (duration > warnDuration && !warningPrinted) {
                log.warn { "Wait time has exceeded $warnDuration" }
                warningPrinted = true
            } else if (duration > interruptDuration) {
                log.error { "Wait time exceeded $interruptDuration, assuming that an error occurred in the generator and cancelling the wait" }
                break
            }
        }

        val endTime = Clock.System.now()
        val diff = endTime - startTime
        log.info { "Waiting for maintenance mode to turn off ended after $diff" }
    }
}
