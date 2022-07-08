package dev.drzepka.wikilinks.generator.flow

class GeneratorFlow<T : FlowStorage>(private val store: T) {

    private val segments = mutableListOf<FlowSegment<T>>()
    private var started = false

    fun step(step: FlowStep<T>) {
        checkNotStarted()

        val segment = object : FlowSegment<T> {
            override val numberOfSteps = 1
            override fun run(store: T, runtime: FlowRuntime) {
                runtime.startNextStep(step.name)
                step.run(store, runtime)
            }
        }

        segment(segment)
    }

    fun segment(segment: FlowSegment<T>) {
        checkNotStarted()
        segments.add(segment)
    }

    fun start() {
        started = true

        val stepCount = segments.sumOf { it.numberOfSteps }
        val flowRuntime = FlowRuntime(stepCount)

        segments.forEach { it.run(store, flowRuntime) }
        flowRuntime.summarize()
    }

    private fun checkNotStarted() {
        if (started)
            throw IllegalStateException("Flow cannot be modified once started")
    }
}
