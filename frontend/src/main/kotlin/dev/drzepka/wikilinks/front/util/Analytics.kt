package dev.drzepka.wikilinks.front.util

import dev.drzepka.wikilinks.common.model.dump.DumpLanguage
import kotlinx.browser.window

sealed class AnalyticsEvent private constructor(val name: String, vararg val additionalData: Pair<String, Any>) {

    class LanguageChanged(fromLang: DumpLanguage, toLang: DumpLanguage) :
        AnalyticsEvent("change_language", "fromLanguage" to fromLang.value, "toLanguage" to toLang.value)

    class GraphTouched(language: DumpLanguage, degreesOfSeparation: Int, linkCount: Int) :
        AnalyticsEvent(
            "touch_graph",
            "language" to language.value,
            "degrees_of_separation" to degreesOfSeparation,
            "link_count" to linkCount
        )

    class ResultDescriptionLinkClicked(language: DumpLanguage) :
        AnalyticsEvent("click_result_description_link", "language" to language.value)

    class GraphLinkClicked(language: DumpLanguage) :
        AnalyticsEvent("click_result_graph_link", "language" to language.value)

    class ListLinkClicked(language: DumpLanguage) :
        AnalyticsEvent("click_result_list_link", "language" to language.value)

    class SearchTimeDetailsShown : AnalyticsEvent("show_search_time_details")
}

/**
 * Helper class to trigger analytics events only once for the same scope.
 */
class ScopedAnalytics<T>(private var scope: T? = null) {
    private val triggeredEvents = mutableSetOf<String>()

    fun updateScope(newScope: T?) {
        if (newScope != scope) {
            scope = newScope
            triggeredEvents.clear()
        }
    }

    fun triggerEvent(event: AnalyticsEvent) {
        if (!canTrigger(event)) return

        triggerAnalyticsEvent(event)
        triggeredEvents.add(event.name)
    }

    private fun canTrigger(event: AnalyticsEvent): Boolean = scope != null && event.name !in triggeredEvents
}

fun triggerAnalyticsEvent(event: AnalyticsEvent) {
    val gtag = window.asDynamic().gtag ?: return

    val eventData = js("{}")
    event.additionalData.forEach {
        eventData[it.first] = it.second
    }

    gtag("event", event.name, eventData)
}
