//
//  Copyright 2023 PLAID, Inc.
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

package io.karte.android.inappmessaging.internal.javascript

import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

private const val EVENT = "event"
private const val STATE_CHANGE = "state_changed"
private const val OPEN_URL = "open_url"
private const val VISIBILITY = "visibility"
private const val DOCUMENT_CHANGED = "document_changed"

internal sealed class JsMessage {
    abstract val name: String

    class Event(val eventName: String, val values: JSONObject) : JsMessage() {
        override val name: String = EVENT
    }

    class StateChanged(val state: String) : JsMessage() {
        override val name: String = STATE_CHANGE
    }

    class OpenUrl(val uri: Uri, target: String) : JsMessage() {
        override val name: String = OPEN_URL
        val withReset: Boolean = target != "_blank"
    }

    class Visibility(val visible: Boolean) : JsMessage() {
        override val name: String = VISIBILITY
    }

    class DocumentChanged(val regions: JSONArray) : JsMessage() {
        override val name: String = DOCUMENT_CHANGED
    }

    companion object {
        fun parse(name: String, data: String): JsMessage? {
            try {
                val dataJson = JSONObject(data)
                return when (name) {
                    EVENT -> {
                        val eventName = dataJson.getString("event_name")
                        val values = dataJson.getJSONObject("values")
                        Event(eventName, values)
                    }
                    STATE_CHANGE -> StateChanged(dataJson.getString("state"))
                    OPEN_URL -> {
                        val uri = Uri.parse(dataJson.getString("url"))
                        val target = dataJson.optString("target")
                        OpenUrl(uri, target)
                    }
                    VISIBILITY -> Visibility(dataJson.getString("state") == "visible")
                    DOCUMENT_CHANGED -> DocumentChanged(dataJson.getJSONArray("touchable_regions"))
                    else -> null
                }
            } catch (_: Exception) {
            }
            return null
        }
    }
}
