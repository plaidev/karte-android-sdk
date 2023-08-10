//
//  Copyright 2023 PLAID, Inc.
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

package io.karte.android.notifications.internal.command

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import io.karte.android.core.library.CommandModule
import io.karte.android.core.logger.Logger
import io.karte.android.notifications.Notifications

private const val LOG_TAG = "Karte.Notifications.Command"
private const val PERMISSION_REQUEST_CODE = 4165000

internal class RegisterPushCommandExecutor : CommandModule {
    override val name: String = "RegisterPushCommand"

    //region CommandModule
    override fun execute(uri: Uri, isDelay: Boolean): Intent? {
        // そもそも権限がないと通知表示ができない。また現状遅延実行処理は通知タップのみ。なので遅延実行処理は未実装。
        // ただし、表示権限がなくても受信はできるので遅延実行処理はリクエストされうる。そのため受信時に発火させないために遅延実行処理でフィルタリングする。
        if (isDelay) return null

        // IAM経由でcurrentActivityが取れる場合のみ即時発火発火する。
        val activity = Notifications.self?.currentActivity?.get() ?: return null
        Logger.d(LOG_TAG, "execute $activity")
        requestPermission(activity)
        // 画面遷移させたくないので、intentは飛ばさない。
        return null
    }

    override fun validate(uri: Uri): Boolean {
        if (super.validate(uri) && uri.host == "register-push") {
            return true
        }
        return false
    }
    //endregion

    private fun requestPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        Logger.d(LOG_TAG, "requestPermission $activity")
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            return
        }
        // 一度拒否されていた場合、事前説明を挟むべきだが、接客で説明済み。ここでは常に表示するためreturnしない。
        // else if (activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
        //     return
        // }
        else {
            activity.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), PERMISSION_REQUEST_CODE)
        }
    }
}
