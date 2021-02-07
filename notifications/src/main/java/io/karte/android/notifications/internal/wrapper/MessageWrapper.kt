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
package io.karte.android.notifications.internal.wrapper

import io.karte.android.notifications.KarteAttributes
import org.json.JSONObject

private const val KEY_ATTRIBUTES = "krt_attributes"
internal const val KEY_PUSH_NOTIFICATION_FLAG = "krt_push_notification"
internal const val KEY_MASS_PUSH_NOTIFICATION_FLAG = "krt_mass_push_notification"
internal const val KEY_EVENT_VALUES = "krt_event_values"
internal const val KEY_CAMPAIGN_ID = "krt_campaign_id"
internal const val KEY_SHORTEN_ID = "krt_shorten_id"

internal class MessageWrapper(val data: Map<String, String>) {
    val attributes: KarteAttributes? by lazy {
        data[KEY_ATTRIBUTES]?.let {
            runCatching { KarteAttributes().load(JSONObject(it)) }.getOrNull()
        }
    }
    val isTargetPush = data[KEY_PUSH_NOTIFICATION_FLAG]?.toBoolean() ?: false
    val isMassPush = data[KEY_MASS_PUSH_NOTIFICATION_FLAG]?.toBoolean() ?: false
    val isKartePush = isTargetPush || isMassPush
    val eventValues = data[KEY_EVENT_VALUES]
    val campaignId = data[KEY_CAMPAIGN_ID]
    val shortenId = data[KEY_SHORTEN_ID]
}
