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
package io.karte.android.inappmessaging.internal

import android.util.Base64
import io.karte.android.core.logger.Logger
import io.karte.android.tracking.client.TrackRequest
import io.karte.android.utilities.forEach
import io.karte.android.utilities.map
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.ArrayList

private const val LOG_TAG = "Karte.IAMessages"

internal class MessageModel @Throws(JSONException::class)
constructor(private val data: JSONObject?, private val request: TrackRequest) {

    val string: String
        get() = Base64.encodeToString(data.toString().toByteArray(), Base64.NO_WRAP)

    val messages: List<JSONObject>
        @Throws(JSONException::class)
        get() {
            val messages = data?.optJSONArray("messages") ?: return listOf()
            return messages.map { it }.filterIsInstance<JSONObject>()
        }

    fun filter(pvId: String, exclude: (JSONObject, String) -> Unit) {
        if (request.pvId == pvId || request.pvId == request.originalPvId) {
            // pageIdが一致 または 未定義の場合は何もしない
            return
        }

        try {
            val messages = data?.optJSONArray("messages") ?: return

            val modifiedMessages = ArrayList<JSONObject>()
            messages.forEach {
                val message = it as? JSONObject ?: return@forEach
                val campaign = message.getJSONObject("campaign")

                val displayLimitMode = campaign.optBoolean("native_app_display_limit_mode", false)
                if (!displayLimitMode) {
                    modifiedMessages.add(message)
                } else {
                    Logger.i(LOG_TAG, "Skip to handle response because screen transited.")
                    exclude(message, "The display is suppressed by native_app_display_limit_mode.")
                }
            }
            data.put("messages", JSONArray(modifiedMessages))
        } catch (e: JSONException) {
            Logger.d(LOG_TAG, "Failed to parse json.", e)
        }
    }

    @Throws(JSONException::class)
    fun shouldLoad(): Boolean {
        return messages.any {
            it.getJSONObject("campaign").getString("service_action_type") != "remote_config"
        }
    }

    fun shouldFocus(): Boolean {
        return try {
            messages.any {
                it.getJSONObject("action").getBoolean("native_app_window_focusable")
            }
        } catch (e: JSONException) {
            Logger.d(LOG_TAG, "Failed to parse json.")
            false
        }
    }

    fun shouldFocusCrossDisplayCampaign(): Boolean {
        return try {
            messages.any {
                it.getJSONObject("action").getBoolean("native_app_window_focusable") &&
                    it.getJSONObject("campaign").getBoolean("native_app_cross_display_mode")
            }
        } catch (e: JSONException) {
            Logger.d(LOG_TAG, "Failed to parse json.")
            false
        }
    }

    override fun toString(): String {
        return "Messages: ${messages.joinToString { message ->
            val action = message.optJSONObject("action")
            JSONObject()
                .put(
                    "action", JSONObject()
                        .put("_id", action?.optString("_id"))
                        .put("shorten_id", action?.optString("shorten_id"))
                )
                .put("campaign", message.optJSONObject("campaign"))
                .toString()
        }}"
    }

    internal interface MessageAdapter {
        fun dequeue(): MessageModel?
    }

    internal interface MessageView {
        var adapter: MessageAdapter?
        fun notifyChanged()
    }
}
