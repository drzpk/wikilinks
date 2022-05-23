package dev.drzepka.wikilinks.generator.flow

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
    private var progressPrinted = false
    private var stepStartTime = Instant.now()
    private var firstStepStartTime: Instant? = null

    private val totalStepsLength = totalSteps.toString().length
    private val progressPadding = " ".repeat(4 + totalStepsLength * 2)

    fun startNextStep(name: String) {
        if (progressPrinted) {
            println()
            progressPrinted = false
        }

        val paddedCurrentStep = (++currentStep).toString().padStart(totalStepsLength)
        println("[$paddedCurrentStep/$totalSteps] $name")

        stepStartTime = Instant.now()
        if (firstStepStartTime == null)
            firstStepStartTime = stepStartTime
    }

    fun summarize() {
        if (firstStepStartTime == null)
            return

        if (progressPrinted) {
            println()
            progressPrinted = false
        }

        val duration = Duration.between(firstStepStartTime!!, Instant.now())
        val hours = if (duration.toHours() > 0) duration.toHours().toString() + "h " else ""
        val minutes = duration.toMinutesPart().toString()
        val seconds = duration.toSecondsPart().toString()

        println()
        println("Total execution time: $hours${minutes}m ${seconds}s")
    }

    override fun updateProgress(current: Int, total: Int, unit: String) {
        val percentage = floor(current.toFloat() / total * 1000) / 10
        val duration = Duration.between(stepStartTime, Instant.now())
        val minutes = duration.toMinutes().toString().padStart(2, '0')
        val seconds = duration.toSecondsPart().toString().padStart(2, '0')

        print("\r$progressPadding$percentage%    $current/$total$unit    $minutes:$seconds    ")
        progressPrinted = true
    }
}

interface ProgressLogger {
    fun updateProgress(current: Int, total: Int, unit: String)
}
