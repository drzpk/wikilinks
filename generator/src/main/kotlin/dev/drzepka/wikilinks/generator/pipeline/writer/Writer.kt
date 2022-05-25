package dev.drzepka.wikilinks.generator.pipeline.writer

interface Writer<T> {
    fun write(value: T)
    fun finalizeWriting()
}
