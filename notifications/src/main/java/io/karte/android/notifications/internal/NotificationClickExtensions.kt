//
//  Copyright 2026 PLAID, Inc.
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

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import io.karte.android.core.logger.Logger

private const val LOG_TAG = "Karte.NotificationClick"

internal fun Context.startNotificationTargetActivity(intent: Intent) {
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Logger.w(LOG_TAG, "Failed to start target activity. Falling back to launcher.", e)
        // 通知生成時に指定された defaultIntent の有無にかかわらず、
        // 起動失敗時はアプリのランチャー画面を固定のフォールバック先とする。
        tryStartAppLauncherActivity()
    }
}

private fun Context.tryStartAppLauncherActivity() {
    val launcherIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (launcherIntent == null) {
        Logger.e(LOG_TAG, "No launcher activity found.")
        return
    }

    try {
        startActivity(launcherIntent)
    } catch (e: ActivityNotFoundException) {
        Logger.e(LOG_TAG, "Failed to start launcher activity.", e)
    }
}
