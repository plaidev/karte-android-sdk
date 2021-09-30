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

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import io.karte.android.core.logger.Logger
import io.karte.android.notifications.MessageReceiveActivity
import io.karte.android.notifications.MessageReceiver
import io.karte.android.tracking.valuesOf

private const val EXTRA_TARGET_PUSH_FLAG = KEY_PUSH_NOTIFICATION_FLAG
private const val EXTRA_MASS_PUSH_FLAG = KEY_MASS_PUSH_NOTIFICATION_FLAG
private const val EXTRA_EVENT_VALUES = KEY_EVENT_VALUES
private const val EXTRA_CAMPAIGN_ID = KEY_CAMPAIGN_ID
private const val EXTRA_SHORTEN_ID = KEY_SHORTEN_ID
private const val EXTRA_EVENT_TYPE = "krt_event_name"
private const val EXTRA_COMPONENT_NAME = "krt_component_name"
internal const val ACTION_KARTE_IGNORED = "io.karte.android.notifications.MESSAGE_IGNORE"

private const val LOG_TAG = "Karte.Notifications.Intent"

internal enum class EventType(val value: String) {
    MESSAGE_CLICK("message_click"), MESSAGE_IGNORE("message_ignore");

    companion object {
        fun of(value: String?): EventType? = values().find { it.value == value }
    }
}

internal class IntentWrapper(val intent: Intent) : MessageWrapper {
    private val targetPushFlag = intent.getStringExtra(EXTRA_TARGET_PUSH_FLAG)
    private val massPushFlag = intent.getStringExtra(EXTRA_MASS_PUSH_FLAG)
    override val isValid = targetPushFlag != null || massPushFlag != null
    val eventType = EventType.of(intent.getStringExtra(EXTRA_EVENT_TYPE))
    override val eventValues = valuesOf(intent.getStringExtra(EXTRA_EVENT_VALUES))
    override val campaignId: String? = intent.getStringExtra(EXTRA_CAMPAIGN_ID)
    override val shortenId: String? = intent.getStringExtra(EXTRA_SHORTEN_ID)
    override val isTargetPush = targetPushFlag == "true"
    override val isMassPush = massPushFlag == "true"

    fun popComponentName() {
        val componentName = intent.getStringExtra(EXTRA_COMPONENT_NAME)
        intent.removeExtra(EXTRA_COMPONENT_NAME)
        if (componentName != null) {
            intent.component = ComponentName.unflattenFromString(componentName)
        } else {
            intent.component = null
        }
    }

    override fun clean() {
        intent.removeExtra(EXTRA_TARGET_PUSH_FLAG)
        intent.removeExtra(EXTRA_MASS_PUSH_FLAG)
        intent.removeExtra(EXTRA_EVENT_VALUES)
        intent.removeExtra(EXTRA_CAMPAIGN_ID)
        intent.removeExtra(EXTRA_SHORTEN_ID)
        intent.removeExtra(EXTRA_EVENT_TYPE)
    }
}

/** Android12以降の通知トランポリン制限のフラグ */
internal fun isTrampolineBlocked(context: Context): Boolean {
    return Build.VERSION.SDK_INT >= 31 && context.applicationInfo.targetSdkVersion >= 31
}

internal class IntentProcessor(
    private val context: Context,
    private val intent: Intent = Intent(),
    private val useActivity: Boolean = false
) {

    private fun pushComponentName() = apply {
        // クリック等計測用にMessageReceiverをsetClassする。
        // Android12以降はReceiverからActivityが起動できない(通知トランポリン制限)ため、Activityを使う
        val receiverClass =
            if (useActivity) MessageReceiveActivity::class.java else MessageReceiver::class.java

        // intentに元々ComponentNameがセットされている場合はMessageReceiverで復元できるようにextraに入れておく。
        val componentName = intent.component
        if (componentName != null) {
            intent.putExtra(EXTRA_COMPONENT_NAME, componentName.flattenToString())
        }
        intent.setClass(context, receiverClass)
    }

    private fun putEventType(type: EventType) = apply {
        Logger.d(LOG_TAG, "put event type: $type")
        intent.putExtra(EXTRA_EVENT_TYPE, type.value)
    }

    private fun copyInfoToIntent(message: RemoteMessageWrapper) = apply {
        Logger.d(LOG_TAG, "copyInfoToIntent() data: ${message.data}, intent: $intent")

        if (message.isTargetPush) {
            intent.putExtra(EXTRA_TARGET_PUSH_FLAG, "true")
            intent.putExtra(EXTRA_CAMPAIGN_ID, message.campaignId)
            intent.putExtra(EXTRA_SHORTEN_ID, message.shortenId)
            intent.putExtra(EXTRA_EVENT_VALUES, message.eventValuesStr)
        } else if (message.isMassPush) {
            intent.putExtra(EXTRA_MASS_PUSH_FLAG, "true")
            intent.putExtra(EXTRA_EVENT_VALUES, message.eventValuesStr)
        }
    }

    fun pendingIntent(uniqueId: Int): PendingIntent {
        var flag = PendingIntent.FLAG_ONE_SHOT
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M)
            flag = flag.or(PendingIntent.FLAG_IMMUTABLE)

        return if (useActivity) {
            PendingIntent.getActivity(context, uniqueId, intent, flag)
        } else {
            PendingIntent.getBroadcast(context, uniqueId, intent, flag)
        }
    }

    companion object {

        fun forClick(
            context: Context,
            message: RemoteMessageWrapper,
            intent: Intent? = null
        ): IntentProcessor {
            val useActivity = isTrampolineBlocked(context)
            val wrapper = if (intent == null) {
                Logger.w(LOG_TAG, "use no launch intent.")
                IntentProcessor(context, useActivity = useActivity)
            } else {
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                IntentProcessor(context, intent, useActivity)
            }
            return wrapper
                .pushComponentName()
                .putEventType(EventType.MESSAGE_CLICK)
                .copyInfoToIntent(message)
        }

        fun forIgnore(context: Context, message: RemoteMessageWrapper): IntentProcessor {
            return IntentProcessor(context, Intent(ACTION_KARTE_IGNORED))
                .pushComponentName()
                .putEventType(EventType.MESSAGE_IGNORE)
                .copyInfoToIntent(message)
        }
    }
}
