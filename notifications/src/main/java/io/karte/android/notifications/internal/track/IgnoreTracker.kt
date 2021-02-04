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
import io.karte.android.notifications.internal.MessageIgnoredEvent
import io.karte.android.notifications.internal.wrapper.IntentWrapper
import io.karte.android.tracking.Tracker
import io.karte.android.tracking.Values

private const val LOG_TAG = "Karte.Notifications.IgnoreTracker"

internal object IgnoreTracker {

    fun sendIfNeeded(wrapper: IntentWrapper?) {
        Logger.d(LOG_TAG, "sendIfNeeded")
        if (wrapper?.isValid != true) return
        try {
            when {
                wrapper.isTargetPush -> trackTargetPush(
                    wrapper.campaignId,
                    wrapper.shortenId,
                    wrapper.eventValues
                )
            }
            wrapper.clean()
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
            "Deleted karte notification. campaignId: $campaignId, shortenId: $shortenId"
        )
        Tracker.track(MessageIgnoredEvent(campaignId, shortenId, eventValues))
    }
}
