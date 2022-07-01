package dev.drzepka.wikilinks.updater.service

import dev.drzepka.wikilinks.updater.Config
import dev.drzepka.wikilinks.updater.aws.EC2Facade
import dev.drzepka.wikilinks.updater.model.InstanceSummary
import software.amazon.awssdk.services.ec2.model.InstanceStateName
import java.time.Duration
import java.time.Instant

class GeneratorDispatcher(private val configRepository: ConfigRepository, private val ec2Facade: EC2Facade) {

    fun handleVersion(version: String?) {
        val summary = ec2Facade.getInstanceSummary(Config.LOCATOR_TAG_VALUE)

        if (version != null) {
            println("New version ($version) has been found, dispatching new generator instance.")
            dispatchNewVersion(summary, version)
        } else {
            println("WikiLinks is using the newest database version available.")
            if (summary != null)
                terminateInstanceIfRunningTooLong(summary)
        }
    }

    private fun dispatchNewVersion(summary: InstanceSummary?, version: String) {
        val instanceAlreadyRunning = summary != null && summary.state != InstanceStateName.TERMINATED
        if (instanceAlreadyRunning) {
            handleExistingInstance(summary!!, version)
        } else {
            launchNewInstance(version)
        }
    }

    private fun handleExistingInstance(summary: InstanceSummary, version: String) {
        if (!terminateInstanceIfRunningTooLong(summary)) {
            val expirationTime = summary.createdAt.plus(Config.GENERATOR_INSTANCE_MAX_AGE)
            val minutesLeft = Duration.between(Instant.now(), expirationTime).toMinutes()
            println("Previous generator instance is still running, and its run time isn't exceeded (has $minutesLeft minutes left).")
        } else {
            launchNewInstance(version)
        }
    }

    private fun terminateInstanceIfRunningTooLong(summary: InstanceSummary): Boolean {
        if (!summary.isMaxAgeExceeded())
            return false

        println("Instance ${summary.id} is running too long, terminating")
        ec2Facade.terminateInstance(summary.id)

        val waitDuration = Duration.ofMinutes(2)
        println("Waiting $waitDuration for the instance to terminate")

        val waitUntil = Instant.now().plus(waitDuration)
        while (Instant.now().isBefore(waitUntil)) {
            ec2Facade.getInstanceSummary(Config.LOCATOR_TAG_VALUE) ?: break
            Thread.sleep(10_000)
            println("Still waiting...")
        }

        ensureMaintenanceModeIsDisabled()
        return true
    }

    private fun ensureMaintenanceModeIsDisabled() {
        if (configRepository.isMaintenanceModeActive()) {
            println("Maintenance mode is still active, deactivating")
            configRepository.deactivateMaintenanceMode()
        }
    }

    private fun launchNewInstance(version: String) {
        val templateId = Config.launchTemplateId
        println("Launching new generator instance with template $templateId")
        ec2Facade.launchInstance(templateId, Config.LOCATOR_TAG_VALUE, version)
    }

    private fun InstanceSummary.isMaxAgeExceeded(): Boolean =
        Instant.now().minus(Config.GENERATOR_INSTANCE_MAX_AGE).isAfter(this.createdAt)
}
