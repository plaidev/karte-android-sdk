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
package io.karte.android.test_lib

import org.json.JSONArray
import org.json.JSONObject

fun createMessage(
    campaignId: String = "sample_campaign",
    shortenId: String = "sample_shorten",
    content: JSONObject = JSONObject(),
    pluginType: String = "webpopup",
    responseTimestamp: String = "sample_response_timestamp",
    noAction: Boolean? = null,
    reason: String? = null,
    triggerEventHash: String = "sample_trigger_event_hash"
): JSONObject {
    val action = JSONObject()
        .put("campaign_id", campaignId)
        .put("shorten_id", shortenId)
        .put("plugin_type", pluginType)
        .put("content", content)
        .put("response_timestamp", responseTimestamp)
    noAction?.let {
        action.put("no_action", it)
    }
    reason?.let {
        action.put("reason", it)
    }

    val campaign = JSONObject()
        .put("_id", campaignId)
        .put("service_action_type", pluginType)
    val trigger = JSONObject()
        .put("event_hashes", triggerEventHash)

    return JSONObject()
        .put("action", action)
        .put("campaign", campaign)
        .put("trigger", trigger)
}

fun createControlGroupMessage(
    campaignId: String = "sample_campaign",
    shortenId: String = "__sample_shorten",
    serviceActionType: String = "webpopup"
): JSONObject {
    val action = JSONObject()
        .put("shorten_id", shortenId)
        .put("type", "control")
    val campaign = JSONObject()
        .put("_id", campaignId)
        .put("service_action_type", serviceActionType)

    return JSONObject().put("action", action).put("campaign", campaign)
}

fun createMessageOpen(
    campaignId: String = "sample_campaign",
    shortenId: String = "sample_shorten"
): JSONObject = JSONObject()
    .put("event_name", "message_open")
    .put(
        "values", JSONObject()
            .put(
                "message", JSONObject()
                    .put("campaign_id", campaignId).put("shorten_id", shortenId)
            )
    )

fun createMessagesResponse(messages: JSONArray): JSONObject {
    return JSONObject().put("response", JSONObject().put("messages", messages))
}

fun createMessageResponse(message: JSONObject): JSONObject {
    return createMessagesResponse(JSONArray().put(message))
}
