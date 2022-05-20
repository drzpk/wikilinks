package dev.drzepka.wikilinks.generator.pipeline.writer

import dev.drzepka.wikilinks.generator.model.Value

interface Writer {
    fun write(value: Value)
    fun finalizeWriting()
}
