package dev.drzepka.wikilinks.generator

import com.google.common.io.CountingInputStream
import dev.drzepka.wikilinks.DatabaseProvider
import dev.drzepka.wikilinks.DatabaseProvider.databaseName
import dev.drzepka.wikilinks.generator.pipeline.PageWriter
import dev.drzepka.wikilinks.generator.pipeline.SqlDumpReader
import dev.drzepka.wikilinks.generator.pipeline.ValueParser
import java.io.File
import java.io.FileInputStream
import java.time.Duration
import java.time.Instant
import kotlin.math.floor

@Suppress("UnstableApiUsage")
fun main() {
    File(databaseName).apply {
        if (isFile)
            delete()
    }

    val db = DatabaseProvider.getDatabase()

    val fileName = "dumps/enwiki-20220501-page.sql.gz"
    val fileSizeMB = File(fileName).length() / 1024 / 1024
    val countingStream = CountingInputStream(FileInputStream(fileName))
    val reader = SqlDumpReader(countingStream)

    val startTime = Instant.now()

    println()
    val writer = PageWriter(db)
    var readItems = 0
    while (reader.hasNext()) {
        val raw = reader.next()
        val value = ValueParser(raw).parse()
        writer.write(value)

        if (readItems++ % 1000 == 0) {
            val readMB = countingStream.count / 1024 / 1024
            val percentage = floor(readMB.toFloat() / fileSizeMB * 1000) / 10
            print("\rProgress: $readMB/$fileSizeMB MB   ($percentage%)    ")
        }
    }

    reader.close()
    println()

    val endTime = Instant.now()
    val duration = Duration.between(startTime, endTime)
    println("Processing time: ${duration.seconds} seconds")
}
