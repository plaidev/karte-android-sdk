//
//  Copyright 2020 PLAID, Inc.
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//      https://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//
package io.karte.android.visualtracking.internal.tracking

import android.view.Window
import io.karte.android.core.logger.Logger
import io.karte.android.utilities.forEach
import io.karte.android.visualtracking.internal.getActionId
import io.karte.android.visualtracking.internal.getTargetText
import io.karte.android.visualtracking.internal.viewFrom
import io.karte.android.visualtracking.internal.viewPathIndices
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.ArrayList

internal class DefinitionList private constructor(
    private val definitions: List<Definition>,
    val lastModified: Long
) {

    @Throws(JSONException::class)
    internal fun traceToEvents(trace: JSONObject, window: Window? = null): List<JSONObject> {
        val ret = ArrayList<JSONObject>()
        for (definition in this.definitions) {
            val event = definition.eventForTrace(trace, window)
            if (event != null) {
                ret.add(event)
            }
        }
        return ret
    }

    override fun toString(): String {
        return "DefinitionList{" +
            "lastModified=" + lastModified +
            ", definitions=" + definitions +
            '}'.toString()
    }

    internal class Definition(private val eventName: String, private val triggers: List<Trigger>) {

        @Throws(JSONException::class)
        fun eventForTrace(trace: JSONObject, window: Window?): JSONObject? {
            for (trigger in triggers) {
                if (!trigger.filter(trace)) continue

                val event = JSONObject()
                    .put("event_name", eventName)
                    .put("values", JSONObject().put("_system", JSONObject().put("auto_track", 1)))

                trigger.fields?.forEach { key, value ->
                    event.getJSONObject("values").put(key, value)
                }
                trigger.dynamicValues(window)?.forEach { key, value ->
                    event.getJSONObject("values").put(key, value)
                }
                return event
            }
            return null
        }

        override fun toString(): String {
            return "Definition{" +
                "eventName='" + eventName + '\''.toString() +
                ", triggers=" + triggers +
                '}'.toString()
        }

        companion object {

            @Throws(JSONException::class)
            fun build(definitionJson: JSONObject): Definition {
                val eventName = definitionJson.getString("event_name")
                val triggersJson = definitionJson.getJSONArray("triggers")
                val triggers = ArrayList<Trigger>()
                for (i in 0 until triggersJson.length()) {
                    val json = triggersJson.getJSONObject(i)
                    try {
                        triggers.add(
                            Trigger(
                                json.optJSONObject("fields"),
                                json.getJSONObject("condition"),
                                json.optJSONArray("dynamic_fields")
                            )
                        )
                    } catch (e: Exception) {
                        Logger.w(LOG_TAG, "Failed to parse auto_track trigger.", e)
                    }
                }
                return Definition(eventName, triggers)
            }
        }
    }

    internal class Trigger @Throws(JSONException::class)
    constructor(internal val fields: JSONObject?, condition: JSONObject, private val dynamicFields: JSONArray?) {
        private val condition: JSONArray = condition.getJSONArray("\$and")
        private val filters = ArrayList<Filter>()

        init {
            for (i in 0 until this.condition.length()) {
                filters.add(Filter.parseQuery(this.condition.getJSONObject(i)))
            }
        }

        @Throws(JSONException::class)
        fun filter(trace: JSONObject): Boolean {
            for (filter in this.filters) {
                if (!filter.filter(trace)) return false
            }
            return true
        }

        fun dynamicValues(window: Window?): JSONObject? {
            if (dynamicFields == null || dynamicFields.length() == 0 || window == null) return null

            var result = JSONObject()
            dynamicFields.forEach { df ->
                if (df !is JSONObject) return@forEach
                val dfActionId = df.optString("action_id")
                val name = df.optString("name")
                if (dfActionId.isEmpty() || name.isEmpty()) return@forEach

                val viewPath = viewPathIndices(dfActionId)
                val view = viewFrom(viewPath, window)
                if (dfActionId == getActionId(view) && view != null) {
                    val targetText = getTargetText(view)
                    result.put(name, targetText)
                }
            }
            return result
        }

        override fun toString(): String {
            return "Trigger{" +
                "fields=" + fields +
                ", condition=" + condition +
                ", dynamic_fields=" + dynamicFields +
                '}'.toString()
        }
    }

    companion object {
        private const val LOG_TAG = "Karte.ATDefinitions"

        @Throws(JSONException::class)
        internal fun buildIfNeeded(body: JSONObject): DefinitionList? {
            if (!body.has("definitions") || "not_modified" == body.optString("status")) {
                return null
            }

            val definitionsJson = body.getJSONArray("definitions")
            val definitions = ArrayList<Definition>()
            for (i in 0 until definitionsJson.length()) {
                definitions.add(Definition.build(definitionsJson.getJSONObject(i)))
            }
            return DefinitionList(definitions, body.getLong("last_modified"))
        }
    }
}
