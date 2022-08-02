package dev.drzepka.wikilinks.app.db

interface ConfigRepository { // todo: move this to the generator module
    fun isGeneratorActive(): Boolean
    fun setGeneratorActive(state: Boolean)
}
