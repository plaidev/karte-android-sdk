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
package io.karte.android.notifications.internal

import io.karte.android.tracking.Event
import io.karte.android.tracking.EventName
import io.karte.android.tracking.Values

private fun messageValues(campaignId: String, shortenId: String, values: Values? = null): Values {
    val message = mapOf("campaign_id" to campaignId, "shorten_id" to shortenId)
    return values?.toMutableMap()?.apply { this["message"] = message }
        ?: mapOf("message" to message)
}

internal class MessageReachedEvent(campaignId: String, shortenId: String, values: Values? = null) :
    Event(PushEventName.MessageReached, messageValues(campaignId, shortenId, values))

internal class MessageIgnoredEvent(campaignId: String, shortenId: String, values: Values? = null) :
    Event(PushEventName.MessageIgnored, messageValues(campaignId, shortenId, values))

internal class MassPushClickEvent(values: Values) :
    Event(PushEventName.MassPushClick, values)

internal class PluginNativeAppIdentifyEvent(subscribe: Boolean, token: String? = null) :
    Event(
        PushEventName.PluginNativeAppIdentify,
        values = mutableMapOf<String, Any>("subscribe" to subscribe).apply {
            if (token != null) this["fcm_token"] = token
        })

private enum class PushEventName(override val value: String) : EventName {
    MessageReached("_message_reached"),
    MessageIgnored("_message_ignored"),
    MassPushClick("mass_push_click"),
    PluginNativeAppIdentify("plugin_native_app_identify"),
}
