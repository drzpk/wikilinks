package dev.drzepka.wikilinks.generator.flow

import dev.drzepka.wikilinks.generator.Configuration
import java.time.Duration
import java.time.Instant
import kotlin.math.floor

/*
[ 1/10] Delete something
[ 2/10] Create blocks
        52.3%    2012/3925MB    00:93
[ 3/10] Do something

Total execution time: 0h 3min 21sec
 */
class Logger(private val totalSteps: Int) : ProgressLogger {

    private var currentStep = 0
    private var needsNewLineAfterStep = false
    private var stepStartTime = Instant.now()
    private var firstStepStartTime: Instant? = null
    private var lastProgressUpdate = Instant.MIN

    private val totalStepsLength = totalSteps.toString().length
    private val progressPadding = " ".repeat(4 + totalStepsLength * 2)

    fun startNextStep(name: String) {
        if (needsNewLineAfterStep) {
            println()
            needsNewLineAfterStep = false
        }

        val paddedCurrentStep = (++currentStep).toString().padStart(totalStepsLength)
        println("[$paddedCurrentStep/$totalSteps] $name")

        stepStartTime = Instant.now()
        lastProgressUpdate = Instant.MIN
        if (firstStepStartTime == null)
            firstStepStartTime = stepStartTime
    }

    fun summarize() {
        if (firstStepStartTime == null)
            return

        if (needsNewLineAfterStep) {
            println()
            needsNewLineAfterStep = false
        }

        val duration = Duration.between(firstStepStartTime!!, Instant.now())
        val hours = if (duration.toHours() > 0) duration.toHours().toString() + "h " else ""
        val minutes = duration.toMinutesPart().toString()
        val seconds = duration.toSecondsPart().toString()

        println()
        println("Total execution time: $hours${minutes}m ${seconds}s")
    }

    override fun updateProgress(current: Int, total: Int, unit: String) {
        if (Configuration.batchMode && lastProgressUpdate.plus(BATCH_MODE_PROGRESS_INTERVAL).isAfter(Instant.now()))
            return

        val percentage = floor(current.toFloat() / total * 1000) / 10
        val duration = Duration.between(stepStartTime, Instant.now())
        val minutes = duration.toMinutes().toString().padStart(2, '0')
        val seconds = duration.toSecondsPart().toString().padStart(2, '0')

        val progressText = "$progressPadding$percentage%    $current/$total$unit    $minutes:$seconds    "
        if (Configuration.batchMode)
            println(progressText)
        else
            print("\r$progressText")

        needsNewLineAfterStep = needsNewLineAfterStep || !Configuration.batchMode
        lastProgressUpdate = Instant.now()
    }

    companion object {
        private val BATCH_MODE_PROGRESS_INTERVAL = Duration.ofMinutes(1)
    }
}

interface ProgressLogger {
    fun updateProgress(current: Int, total: Int, unit: String)
}
