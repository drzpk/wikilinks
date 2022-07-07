package dev.drzepka.wikilinks.generator

import dev.drzepka.wikilinks.app.db.ConfigRepository
import dev.drzepka.wikilinks.app.db.FileConfigRepository
import dev.drzepka.wikilinks.generator.version.UpdateChecker
import java.io.File
import kotlin.concurrent.thread
import kotlin.system.exitProcess


private val workingDirectory = File(Configuration.workingDirectory)
private var turnOffGeneratorFlagOnExit = true

fun main(args: Array<String>) {
    if (!workingDirectory.isDirectory)
        workingDirectory.mkdir()

    val configRepository = FileConfigRepository(workingDirectory.absolutePath)
    val updateChecker = UpdateChecker(configRepository)

    registerShutdownHook(configRepository)

    if (configRepository.isGeneratorActive()) {
        println("Previous generator instance is still working")
        turnOffGeneratorFlagOnExit = false
        return
    }

    val newVersion = if (args.isNotEmpty()) {
        println("Exact version was specified in program arguments")
        args[0]
    } else {
        updateChecker.getNewVersion()
    }

    val result: Boolean = if (newVersion != null) {
        println("Found new version: $newVersion")
        startGenerator(newVersion, configRepository)
    } else {
        println("No new version was found")
        true
    }

    if (!result)
        exitProcess(1)
}

private fun startGenerator(version: String, configRepository: ConfigRepository): Boolean {
    return try {
        configRepository.setGeneratorActive(true)
        generate(version)
        true
    } catch (e: Throwable) {
        println("Uncaught exception occurred in generator")
        e.printStackTrace()
        false
    } finally {
        configRepository.setGeneratorActive(false)
    }
}

private fun registerShutdownHook(configRepository: ConfigRepository) {
    val thread = thread(start = false) {
        println("Shutdown request received, switching off generator flag")
        configRepository.setGeneratorActive(false)
    }
    Runtime.getRuntime().addShutdownHook(thread)
}
