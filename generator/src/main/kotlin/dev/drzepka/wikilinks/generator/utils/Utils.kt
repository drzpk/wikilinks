package dev.drzepka.wikilinks.generator.utils

import dev.drzepka.wikilinks.generator.Configuration
import java.io.File
import java.lang.management.ManagementFactory

private val generatorActiveFile = File(Configuration.workingDirectory + "/generator_active")

fun availableProcessors(): Int = Runtime.getRuntime().availableProcessors()

fun availableHeap(): Long = ManagementFactory.getMemoryMXBean().heapMemoryUsage.max

fun getCmdArgument(args: Array<String>, name: String): String? = args
    .lastOrNull { it.startsWith("$name=") }
    ?.substringAfter('=')

fun isGeneratorActive(): Boolean = generatorActiveFile.isFile

fun setGeneratorActive(state: Boolean) =
    if (state) generatorActiveFile.createNewFile() else generatorActiveFile.delete()
