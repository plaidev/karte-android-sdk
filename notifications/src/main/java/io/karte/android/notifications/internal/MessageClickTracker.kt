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

import android.annotation.TargetApi
import android.content.Intent
import android.os.Build
import io.karte.android.core.library.DeepLinkModule
import io.karte.android.core.logger.Logger
import io.karte.android.notifications.EXTRA_CAMPAIGN_ID
import io.karte.android.notifications.EXTRA_EVENT_VALUES
import io.karte.android.notifications.EXTRA_SHORTEN_ID
import io.karte.android.notifications.KARTE_MASS_PUSH_NOTIFICATION_FLAG
import io.karte.android.notifications.KARTE_PUSH_NOTIFICATION_FLAG
import io.karte.android.notifications.MassPushClickEvent
import io.karte.android.tracking.MessageEvent
import io.karte.android.tracking.MessageEventType
import io.karte.android.tracking.Tracker
import io.karte.android.tracking.Values
import io.karte.android.tracking.valuesOf

private const val LOG_TAG = "Karte.MessageClickTracker"

internal object MessageClickTracker : DeepLinkModule {

    //region DeepLinkModule
    override val name: String = MessageClickTracker::class.java.simpleName

    override fun handle(intent: Intent?) {
        sendMessageClickIfNeeded(intent)
    }
    //endregion

    @TargetApi(Build.VERSION_CODES.KITKAT)
    fun sendMessageClickIfNeeded(intent: Intent?) {
        try {
            if (intent == null || intent.extras == null) {
                return
            }

            val sentByKartePushNotification = intent.getStringExtra(KARTE_PUSH_NOTIFICATION_FLAG)
            val sentByKarteMassPushNotification =
                intent.getStringExtra(KARTE_MASS_PUSH_NOTIFICATION_FLAG)
            val eventValues = valuesOf(intent.getStringExtra(EXTRA_EVENT_VALUES))

            if (sentByKartePushNotification == "true") {
                val campaignId = intent.getStringExtra(EXTRA_CAMPAIGN_ID)
                val shortenId = intent.getStringExtra(EXTRA_SHORTEN_ID)
                trackTargetPushClick(campaignId, shortenId, eventValues)
            } else if (sentByKarteMassPushNotification == "true") {
                trackMassPushClick(eventValues)
            }
            removeExtras(intent)
        } catch (e: Exception) {
            Logger.e(LOG_TAG, "Failed to handle push notification message_click.", e)
        }
    }

    private fun trackTargetPushClick(campaignId: String?, shortenId: String?, eventValues: Values) {
        if (campaignId == null || shortenId == null || eventValues.isEmpty()) return
        Logger.i(
            LOG_TAG,
            "An Activity started by clicking karte notification." +
                " campaignId: $campaignId, shortenId: $shortenId"
        )
        Tracker.track(MessageEvent(MessageEventType.Click, campaignId, shortenId, eventValues))
    }

    private fun trackMassPushClick(eventValues: Values) {
        if (eventValues.isEmpty()) return
        Logger.i(
            LOG_TAG,
            "An Activity started by clicking karte mass push notification." +
                " event values: $eventValues"
        )
        Tracker.track(MassPushClickEvent(eventValues))
    }

    private fun removeExtras(intent: Intent) {
        intent.removeExtra(KARTE_PUSH_NOTIFICATION_FLAG)
        intent.removeExtra(KARTE_MASS_PUSH_NOTIFICATION_FLAG)
        intent.removeExtra(EXTRA_EVENT_VALUES)
        intent.removeExtra(EXTRA_CAMPAIGN_ID)
        intent.removeExtra(EXTRA_SHORTEN_ID)
    }
}
