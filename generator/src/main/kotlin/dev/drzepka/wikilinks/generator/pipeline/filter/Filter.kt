package dev.drzepka.wikilinks.generator.pipeline.filter

interface Filter<T> {
    fun filter(value: T): Boolean
}
