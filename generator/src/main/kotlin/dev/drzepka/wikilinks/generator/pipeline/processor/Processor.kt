package dev.drzepka.wikilinks.generator.pipeline.processor

interface Processor<T> {
    fun process(value: T): T?
}
