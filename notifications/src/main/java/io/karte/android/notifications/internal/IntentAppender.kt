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

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.net.Uri
import io.karte.android.core.logger.Logger
import io.karte.android.notifications.Notifications
import io.karte.android.notifications.internal.wrapper.IntentProcessor
import io.karte.android.notifications.internal.wrapper.RemoteMessageWrapper

private const val LOG_TAG = "Karte.Notification.Intent"

internal object IntentAppender {

    private fun makeClickIntent(context: Context, link: String, defaultIntent: Intent?): Intent? {
        val packageManager = context.packageManager
        // CATEGORY_INFO, CATEGORY_LAUNCHERに該当するActivityがない場合はnull
        val fallbackIntent =
            defaultIntent ?: packageManager.getLaunchIntentForPackage(context.packageName)

        var intent: Intent? = null
        if (link.isNotEmpty()) {
            val uri = Uri.parse(link)
            intent = Notifications.self?.app?.executeCommand(uri, true)?.filterIsInstance<Intent>()
                ?.firstOrNull()
            if (intent == null) {
                intent = Intent(Intent.ACTION_VIEW, uri)
                if (intent.resolveActivity(packageManager) == null) {
                    Logger.w(
                        LOG_TAG,
                        "Cannot resolve specified link. Trying to use default Activity. : $uri"
                    )
                    intent = null
                }
            }
        }
        intent = intent ?: fallbackIntent

        if (intent == null) {
            Logger.w(LOG_TAG, "No Activity to launch was found.")
        }
        return intent
    }

    fun append(
        notification: Notification,
        context: Context,
        uniqueId: Int,
        message: RemoteMessageWrapper,
        defaultIntent: Intent?
    ) {
        val intent = makeClickIntent(context, message.attributes?.link ?: "", defaultIntent)
        notification.contentIntent =
            IntentProcessor.forClick(context, message, intent).pendingIntent(uniqueId)
        notification.deleteIntent =
            IntentProcessor.forIgnore(context, message).pendingIntent(uniqueId)
    }
}
