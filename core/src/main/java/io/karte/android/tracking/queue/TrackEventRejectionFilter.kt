package io.karte.android.tracking.queue

import io.karte.android.tracking.Event

internal class TrackEventRejectionFilter {
    var ruleContainer: MutableMap<String, MutableMap<String, MutableList<TrackEventRejectionFilterRule>>> = mutableMapOf()

    fun reject(event: Event): Boolean {
        val libraryName = event.libraryName ?: return false
        val subRuleContainer = ruleContainer[libraryName] ?: return false
        val rules = subRuleContainer[event.eventName.value] ?: return false
        return rules.any { it.reject(event) }
    }

    fun add(rule: TrackEventRejectionFilterRule) {
        ruleContainer.getOrPut(rule.libraryName) {
            mutableMapOf()
        }.getOrPut(rule.eventName.value) {
            mutableListOf()
        }.add(rule)
    }
}
