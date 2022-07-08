package dev.drzepka.wikilinks.generator.flow

interface ProgressLogger {
    fun updateProgress(current: Int, total: Int, unit: String)
}
