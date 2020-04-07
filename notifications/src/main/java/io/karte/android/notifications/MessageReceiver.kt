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
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import io.karte.android.core.logger.Logger
import io.karte.android.notifications.internal.MessageClickTracker

private const val LOG_TAG = "Karte.MessageReceiver"

@RequiresApi(api = Build.VERSION_CODES.KITKAT)
internal class MessageReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Logger.d(LOG_TAG, "Received notification click. Intent=$intent")
        MessageClickTracker.sendMessageClickIfNeeded(intent)

        val componentName = intent.getStringExtra(EXTRA_COMPONENT_NAME)
        intent.removeExtra(EXTRA_COMPONENT_NAME)
        if (componentName != null) {
            intent.component = ComponentName.unflattenFromString(componentName)
        } else {
            intent.component = null
        }

        context.startActivity(intent)
    }
}
