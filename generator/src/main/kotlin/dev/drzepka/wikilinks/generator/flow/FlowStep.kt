package dev.drzepka.wikilinks.generator.flow

interface FlowStep<T> {
    val name: String
    fun run(store: T, logger: ProgressLogger)
}
