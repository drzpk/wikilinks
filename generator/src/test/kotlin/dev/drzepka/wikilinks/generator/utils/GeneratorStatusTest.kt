package dev.drzepka.wikilinks.generator.utils

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GeneratorStatusTest {
    private val tmpDirectory = File(System.getProperty("java.io.tmpdir"))

    @BeforeAll
    @AfterAll
    fun cleanup() {
        tmpDirectory.listFiles()!!
            .filter { it.isFile && it.startsWith(FILE_PREFIX) && it.endsWith(FILE_SUFFIX) }
            .forEach { it.delete() }
    }

    @Test
    fun `should detect generator activation if there is a file`() {
        val file = getTempFile()
        val generatorStatus = GeneratorStatus.fromFile(file)

        file.createNewFile()

        assertTrue(generatorStatus.isGeneratorActive())
    }

    @Test
    fun `should not detect activation if file is too old`() {
        val maxAge = 30.minutes
        val file = getTempFile()
        val generatorStatus = GeneratorStatus.fromFile(file, maxAge)

        file.createNewFile()
        file.setLastModified(System.currentTimeMillis() - (maxAge + 1.seconds).inWholeMilliseconds)

        assertFalse(generatorStatus.isGeneratorActive())
    }

    private fun getTempFile(): File {
        val random = Random.nextInt(1_000_000, 100_000_000)
        val name = "$FILE_PREFIX$random$FILE_SUFFIX"
        val file = File(tmpDirectory, name)
        file.deleteOnExit()
        return file
    }

    companion object {
        private const val FILE_PREFIX = "wikilinks-generator-status-test"
        private const val FILE_SUFFIX = ".tmp"
    }
}
