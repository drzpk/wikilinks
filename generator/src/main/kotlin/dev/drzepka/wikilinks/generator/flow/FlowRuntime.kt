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

Execution time summary
  1. Test:      3m 2s
  2. Step name: 22s
Total execution time: 0h 3min 21sec
 */
class FlowRuntime(private val totalSteps: Int) : ProgressLogger {

    private var currentStep = 0
    private var needsNewLineAfterStep = false
    private var stepStartTime = Instant.now()
    private var lastProgressUpdate = Instant.MIN

    private val totalStepsLength = totalSteps.toString().length
    private val progressPadding = " ".repeat(4 + totalStepsLength * 2)
    private val stepExecutionTimePoints = mutableListOf<Pair<String, Instant>>()

    fun startNextStep(name: String) {
        if (needsNewLineAfterStep) {
            println()
            needsNewLineAfterStep = false
        }

        val paddedCurrentStep = (++currentStep).toString().padStart(totalStepsLength)
        println("[$paddedCurrentStep/$totalSteps] $name")

        stepStartTime = Instant.now()
        stepExecutionTimePoints.add(Pair(name, stepStartTime))
        lastProgressUpdate = Instant.MIN
    }

    fun summarize() {
        if (stepExecutionTimePoints.isEmpty())
            return

        if (needsNewLineAfterStep) {
            println()
            needsNewLineAfterStep = false
        }

        val labelWidth = stepExecutionTimePoints.maxOf { it.first.length } + ":".length
        val stepNoWidth = stepExecutionTimePoints.size.toString().length
        val timePoints = stepExecutionTimePoints.map { it.second }.toMutableList().apply { add(Instant.now()) }

        println()
        println("Execution time summary:")
        timePoints
            .windowed(2)
            .map { Duration.between(it[0], it[1]).format(padZeros = true) }
            .forEachIndexed { index, time ->
                val no = (index + 1).toString().padStart(stepNoWidth, ' ')
                val label = (stepExecutionTimePoints[index].first + ":").padEnd(labelWidth, ' ')
                println("  $no. $label $time")
            }

        val totalStartPadding = " ".repeat(2 + stepNoWidth + 2)
        val totalLabel = "TOTAL:".padEnd(labelWidth - "00h ".length, ' ')
        val totalDuration = Duration.between(timePoints.first(), timePoints.last()).format(
            includeHours = true,
            padZeros = true
        )
        println("$totalStartPadding$totalLabel $totalDuration")
    }

    override fun updateProgress(current: Int, total: Int, unit: String) {
        if (Configuration.batchMode && lastProgressUpdate.plus(BATCH_MODE_PROGRESS_INTERVAL).isAfter(Instant.now()))
            return

        val percentage = floor(current.toFloat() / total * 1000) / 10
        val duration = Duration.between(stepStartTime, Instant.now())

        val progressText = "$progressPadding$percentage%    $current/$total$unit    ${duration.format()}    "
        if (Configuration.batchMode)
            println(progressText)
        else
            print("\r$progressText")

        needsNewLineAfterStep = needsNewLineAfterStep || !Configuration.batchMode
        lastProgressUpdate = Instant.now()
    }

    private fun Duration.format(includeHours: Boolean = false, padZeros: Boolean = false): String {
        var hours = ""


        if (includeHours) {
            hours += toHours().toString()
            if (padZeros)
                hours = hours.padStart(2, '0')
            hours += "h "
        }

        var minutes = (if (includeHours) toMinutesPart() else toMinutes()).toString()
        if (padZeros)
            minutes = minutes.padStart(2, '0')
        minutes += "m "

        var seconds = toSecondsPart().toString()
        if (padZeros)
            seconds = seconds.padStart(2, '0')
        seconds += "s"

        return "$hours$minutes$seconds"
    }

    companion object {
        private val BATCH_MODE_PROGRESS_INTERVAL = Duration.ofMinutes(1)
    }
}
