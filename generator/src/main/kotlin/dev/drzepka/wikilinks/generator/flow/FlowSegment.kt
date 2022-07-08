package dev.drzepka.wikilinks.generator.flow

interface FlowSegment<T> {
    val numberOfSteps: Int
    fun run(store: T, runtime: FlowRuntime)
}
