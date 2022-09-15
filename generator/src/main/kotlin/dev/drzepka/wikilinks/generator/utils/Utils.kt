package dev.drzepka.wikilinks.generator.utils

import java.lang.management.ManagementFactory

fun availableProcessors(): Int = Runtime.getRuntime().availableProcessors()

fun availableHeap(): Long = ManagementFactory.getMemoryMXBean().heapMemoryUsage.max

fun getCmdArgument(args: Array<String>, name: String): String? = args
    .lastOrNull { it.startsWith("$name=") }
    ?.substringAfter('=')
