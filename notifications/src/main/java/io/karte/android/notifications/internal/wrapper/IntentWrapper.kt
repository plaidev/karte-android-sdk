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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import io.karte.android.core.logger.Logger
import io.karte.android.notifications.MessageReceiver
import io.karte.android.tracking.valuesOf

private const val EXTRA_TARGET_PUSH_FLAG = KEY_PUSH_NOTIFICATION_FLAG
private const val EXTRA_MASS_PUSH_FLAG = KEY_MASS_PUSH_NOTIFICATION_FLAG
private const val EXTRA_EVENT_VALUES = KEY_EVENT_VALUES
private const val EXTRA_CAMPAIGN_ID = KEY_CAMPAIGN_ID
private const val EXTRA_SHORTEN_ID = KEY_SHORTEN_ID
private const val EXTRA_EVENT_TYPE = "krt_event_name"
private const val EXTRA_COMPONENT_NAME = "krt_component_name"

private const val LOG_TAG = "Karte.Notifications.Intent"

internal enum class EventType(val value: String) {
    MESSAGE_CLICK("message_click"), MESSAGE_IGNORE("message_ignore");

    companion object {
        fun of(value: String?): EventType? = values().find { it.value == value }
    }
}

internal class IntentWrapper(val intent: Intent) {
    private val targetPushFlag = intent.getStringExtra(EXTRA_TARGET_PUSH_FLAG)
    private val massPushFlag = intent.getStringExtra(EXTRA_MASS_PUSH_FLAG)
    val isValid = targetPushFlag != null || massPushFlag != null
    val eventType = EventType.of(intent.getStringExtra(EXTRA_EVENT_TYPE))
    val eventValues = valuesOf(intent.getStringExtra(EXTRA_EVENT_VALUES))
    val campaignId: String? = intent.getStringExtra(EXTRA_CAMPAIGN_ID)
    val shortenId: String? = intent.getStringExtra(EXTRA_SHORTEN_ID)
    val isTargetPush = targetPushFlag == "true"
    val isMassPush = massPushFlag == "true"

    fun popComponentName() {
        val componentName = intent.getStringExtra(EXTRA_COMPONENT_NAME)
        intent.removeExtra(EXTRA_COMPONENT_NAME)
        if (componentName != null) {
            intent.component = ComponentName.unflattenFromString(componentName)
        } else {
            intent.component = null
        }
    }

    fun clean() {
        intent.removeExtra(EXTRA_TARGET_PUSH_FLAG)
        intent.removeExtra(EXTRA_MASS_PUSH_FLAG)
        intent.removeExtra(EXTRA_EVENT_VALUES)
        intent.removeExtra(EXTRA_CAMPAIGN_ID)
        intent.removeExtra(EXTRA_SHORTEN_ID)
        intent.removeExtra(EXTRA_EVENT_TYPE)
    }

    companion object {

        private fun putEventType(intent: Intent, type: EventType) {
            Logger.d(LOG_TAG, "put event type: $type")
            intent.putExtra(EXTRA_EVENT_TYPE, type.value)
        }

        private fun pushComponentName(intent: Intent, context: Context) {
            // MessageReceiverでBroadcastを受けるためにsetClassする。
            // intentに元々ComponentNameがセットされている場合はMessageReceiverで復元できるようにextraに入れておく。
            val componentName = intent.component
            if (componentName != null) {
                intent.putExtra(EXTRA_COMPONENT_NAME, componentName.flattenToString())
            }
            intent.setClass(context, MessageReceiver::class.java)
        }

        private fun copyInfoToIntent(intent: Intent, message: MessageWrapper): Intent {
            Logger.d(LOG_TAG, "copyInfoToIntent() data: ${message.data}, intent: $intent")

            if (message.isTargetPush) {
                intent.putExtra(EXTRA_TARGET_PUSH_FLAG, "true")
                intent.putExtra(EXTRA_CAMPAIGN_ID, message.campaignId)
                intent.putExtra(EXTRA_SHORTEN_ID, message.shortenId)
                intent.putExtra(EXTRA_EVENT_VALUES, message.eventValues)
            } else if (message.isMassPush) {
                intent.putExtra(EXTRA_MASS_PUSH_FLAG, "true")
                intent.putExtra(EXTRA_EVENT_VALUES, message.eventValues)
            }
            return intent
        }

        fun wrapIntent(
            context: Context,
            message: MessageWrapper,
            eventType: EventType,
            intent: Intent? = null
        ): Intent {
            val ret = if (intent == null) {
                Logger.w(LOG_TAG, "use no launch intent.")
                Intent(context, MessageReceiver::class.java)
            } else {
                pushComponentName(intent, context)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                intent
            }
            putEventType(ret, eventType)
            copyInfoToIntent(ret, message)
            return ret
        }
    }
}
