package dev.drzepka.wikilinks.generator.pipeline.relocator.mover

import dev.drzepka.wikilinks.generator.flow.ProgressLogger
import java.io.File

interface FileMover {
    fun move(file: File, logger: ProgressLogger)
}
