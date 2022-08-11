package dev.drzepka.wikilinks.generator.pipeline.relocator.mover

import dev.drzepka.wikilinks.generator.flow.ProgressLogger
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

class LocalFilesystemMover(private val targetDirectory: File) : FileMover {

    override fun move(file: File, logger: ProgressLogger) {
        if (!targetDirectory.isDirectory && !targetDirectory.mkdirs())
            throw IllegalStateException("Unable to create directory $targetDirectory")

        Files.move(
            file.toPath(),
            Paths.get(targetDirectory.absolutePath, file.name),
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING
        )
    }
}
