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
package io.karte.android.notifications.internal.track

import io.karte.android.core.logger.Logger
import io.karte.android.notifications.internal.MessageReachedEvent
import io.karte.android.notifications.internal.wrapper.MessageWrapper
import io.karte.android.tracking.Tracker
import io.karte.android.tracking.Values
import io.karte.android.tracking.valuesOf

private const val LOG_TAG = "Karte.Notifications.ReachedTracker"

internal object ReachedTracker {

    fun sendIfNeeded(wrapper: MessageWrapper?) {
        Logger.d(LOG_TAG, "sendIfNeeded")
        if (wrapper?.attributes == null) return
        try {
            when {
                wrapper.isTargetPush -> trackTargetPush(
                    wrapper.campaignId,
                    wrapper.shortenId,
                    valuesOf(wrapper.eventValues)
                )
            }
        } catch (e: Exception) {
            Logger.e(LOG_TAG, "Failed to handle push notification _message_ignored.", e)
        }
    }

    private fun trackTargetPush(
        campaignId: String?,
        shortenId: String?,
        eventValues: Values
    ) {
        if (campaignId == null || shortenId == null || eventValues.isEmpty()) return
        Logger.i(
            LOG_TAG,
            "Reached karte notification. campaignId: $campaignId, shortenId: $shortenId"
        )
        Tracker.track(MessageReachedEvent(campaignId, shortenId, eventValues))
    }
}
