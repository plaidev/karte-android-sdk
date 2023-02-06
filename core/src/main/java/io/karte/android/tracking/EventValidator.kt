//
//  Copyright 2021 PLAID, Inc.
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

package io.karte.android.tracking

import io.karte.android.utilities.isAscii
import io.karte.android.utilities.toMap
import org.json.JSONObject
import java.util.regex.Pattern

private val EVENT_NAME_REGEX = Pattern.compile("[^a-z0-9_]")
internal val INVALID_FIELD_NAMES = listOf(
    "_source",
    "_system",
    "any",
    "avg",
    "cache",
    "count",
    "count_sets",
    "date",
    "f_t",
    "first",
    "keys",
    "l_t",
    "last",
    "lrus",
    "max",
    "min",
    "o",
    "prev",
    "sets",
    "size",
    "span",
    "sum",
    "type",
    "v"
)

internal object EventValidator {

    internal fun getDeprecatedMessages(event: Event): List<String> {
        val eventName = event.eventName.value
        val messages = mutableListOf<String>()

        if (!eventName.isAscii())
            messages.add("Multi-byte character in event name is deprecated: Event=$eventName")

        if (validateEventName(eventName))
            messages.add(
                "[^a-z0-9_] or starting with _ in event name is deprecated:" +
                    " Event=$eventName"
            )

        if (validateEventFieldName(event.values))
            messages.add(
                "Contains dots(.) or stating with $ or $INVALID_FIELD_NAMES" +
                    " in event field name is deprecated:" +
                    " EventName=$eventName,FieldName=${event.values}"
            )
        return messages
    }

    internal fun getInvalidMessages(event: Event): List<String> {
        val messages = mutableListOf<String>()
        if (validateEventFieldValue(event.eventName.value, event.values))
            messages.add(
                "view_name or user_id is empty:" +
                    " EventName=${event.eventName.value},FieldName=${event.values}"
            )
        return messages
    }

    /** 非推奨なイベント名が含まれるかを返します。 */
    private fun validateEventName(eventName: String): Boolean {
        if (eventName.isEmpty()) {
            return false
        }
        when (eventName) {
            MessageEventName.MessageReady.value,
            MessageEventName.MessageSuppressed.value,
            "_fetch_variables" -> {
                return false
            }
        }
        val m = EVENT_NAME_REGEX.matcher(eventName)
        return m.find() || eventName.startsWith("_")
    }

    /** 非推奨なフィールド名が含まれるかを返します。 */
    private fun validateEventFieldName(values: JSONObject): Boolean {
        if (values.length() == 0) {
            return false
        }
        return values.toMap().any {
            it.key.startsWith("$") || it.key.contains(".") || INVALID_FIELD_NAMES.contains(it.key)
        }
    }

    /** 無効な値が含まれるかを返します。 */
    private fun validateEventFieldValue(eventName: String, values: JSONObject): Boolean {
        if (values.length() == 0) {
            return false
        }

        when (eventName) {
            BaseEventName.View.value -> {
                val viewName = values.optString("view_name")
                if (viewName.isEmpty()) {
                    return true
                }
            }
            BaseEventName.Identify.value -> {
                if (values.has("user_id") && values.optString("user_id").isEmpty()) {
                    return true
                }
            }
        }
        return false
    }
}
