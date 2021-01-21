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

import android.content.Intent
import io.karte.android.core.library.DeepLinkModule
import io.karte.android.core.logger.Logger
import io.karte.android.notifications.internal.MassPushClickEvent
import io.karte.android.notifications.internal.wrapper.IntentWrapper
import io.karte.android.tracking.MessageEvent
import io.karte.android.tracking.MessageEventType
import io.karte.android.tracking.Tracker
import io.karte.android.tracking.Values

private const val LOG_TAG = "Karte.Notifications.ClickTracker"

internal object ClickTracker : DeepLinkModule {

    //region DeepLinkModule
    override val name: String = "NotificationsClickTracker"

    override fun handle(intent: Intent?) {
        Logger.d(LOG_TAG, "handle deeplink")
        sendIfNeeded(intent?.let { IntentWrapper(intent) })
    }
    //endregion

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
                wrapper.isMassPush -> trackMassPush(wrapper.eventValues)
            }
            wrapper.clean()
        } catch (e: Exception) {
            Logger.e(LOG_TAG, "Failed to handle push notification message_click.", e)
        }
    }

    private fun trackTargetPush(campaignId: String?, shortenId: String?, eventValues: Values) {
        if (campaignId == null || shortenId == null || eventValues.isEmpty()) return
        Logger.i(
            LOG_TAG,
            "An Activity started by clicking karte notification." +
                " campaignId: $campaignId, shortenId: $shortenId"
        )
        Tracker.track(MessageEvent(MessageEventType.Click, campaignId, shortenId, eventValues))
    }

    private fun trackMassPush(eventValues: Values) {
        if (eventValues.isEmpty()) return
        Logger.i(
            LOG_TAG,
            "An Activity started by clicking karte mass push notification." +
                " event values: $eventValues"
        )
        Tracker.track(MassPushClickEvent(eventValues))
    }
}
