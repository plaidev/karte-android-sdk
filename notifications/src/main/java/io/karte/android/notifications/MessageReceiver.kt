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
package io.karte.android.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.karte.android.core.logger.Logger
import io.karte.android.notifications.internal.track.ClickTracker
import io.karte.android.notifications.internal.track.IgnoreTracker
import io.karte.android.notifications.internal.wrapper.EventType
import io.karte.android.notifications.internal.wrapper.IntentWrapper

private const val LOG_TAG = "Karte.MessageReceiver"

internal class MessageReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Logger.d(LOG_TAG, "Received notification click. Intent=$intent")

        val wrapper = IntentWrapper(intent)
        if (wrapper.eventType == EventType.MESSAGE_IGNORE) {
            IgnoreTracker.sendIfNeeded(wrapper)
            return
        } else {
            ClickTracker.sendIfNeeded(wrapper)
        }
        wrapper.popComponentName()
        context.startActivity(wrapper.intent)
    }
}
