package dev.drzepka.wikilinks.generator

import dev.drzepka.wikilinks.common.BuildConfig
import dev.drzepka.wikilinks.common.model.dump.DumpLanguage
import dev.drzepka.wikilinks.generator.version.UpdateChecker
import java.io.File
import kotlin.concurrent.thread
import kotlin.system.exitProcess


private val workingDirectory = File(Configuration.workingDirectory)
private val updateChecker = UpdateChecker(workingDirectory)

fun main(args: Array<String>) {
    if (!workingDirectory.isDirectory)
        workingDirectory.mkdir()

    println("WikiLinks Generator ${BuildConfig.VERSION}")
    println("Available CPUs: ${availableProcessors()}")
    println("Max heap: ${availableHeap()}")
    println()

    if (isGeneratorActive()) {
        println("Previous generator instance is still working")
        exitProcess(1)
    }

    registerShutdownHook()

    val languages = getLanguages(args)
    if (languages.isEmpty())
        exitProcess(1)

    val versionUpdates = getVersions(args, languages)
    if (versionUpdates.isEmpty()) {
        println("Nothing to update")
        return
    }

    setGeneratorActive(true)
    for (versionUpdate in versionUpdates) {
        val status = startGenerator(versionUpdate.key, versionUpdate.value)
        if (status)
            updateChecker.setVersion(versionUpdate.key, versionUpdate.value)
        else
            exitProcess(1)
    }
}

private fun getLanguages(args: Array<String>): List<DumpLanguage> {
    val languagesArg = getCmdArgument(args, "language")
    if (languagesArg == null || languagesArg.isBlank()) {
        println("At least one language is required")
        return emptyList()
    }

    val list = mutableListOf<DumpLanguage>()
    for (raw in languagesArg.split(',')) {
        val parsed = DumpLanguage.fromString(raw)
        if (parsed == null) {
            val supportedLanguages = DumpLanguage.values().joinToString(separator = ",") { it.value }
            println("\"raw\" is not a valid language. Supported languages: $supportedLanguages")
            return emptyList()
        }

        list.add(parsed)
    }

    return list
}

private fun getVersions(args: Array<String>, languages: List<DumpLanguage>): Map<DumpLanguage, String> {
    val versionArg = getCmdArgument(args, "version")
    return if (versionArg != null) {
        println("Exact version was specified in program arguments: $versionArg")
        languages.associateWith { versionArg }
    } else {
        updateChecker.getNewVersions(languages)
    }
}

private fun startGenerator(language: DumpLanguage, version: String): Boolean {
    return try {
        println()
        generate(language, version)
        println()
        true
    } catch (e: Throwable) {
        println("Uncaught exception occurred during generator execution")
        e.printStackTrace()
        false
    }
}

private fun registerShutdownHook() {
    val thread = thread(start = false) {
        println("\nShutting down the generator")
        setGeneratorActive(false)
    }
    Runtime.getRuntime().addShutdownHook(thread)
}
