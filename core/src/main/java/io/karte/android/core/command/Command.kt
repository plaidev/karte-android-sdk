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
package io.karte.android.core.command

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import io.karte.android.KarteApp

internal object CommandExecutor {
    fun execute(uri: Uri): List<Any?> {
        val commands = listOf(
            OpenSettingsCommand(),
            OpenStoreCommand()
        )
        return commands.filter { it.validate(uri) }.map { it.execute() }
    }
}

private const val karteSchemeLength = 36

/**
 * コマンドを表現するインターフェースです。
 *
 * **SDK内部で利用するタイプであり、通常のSDK利用でこちらのタイプを利用することはありません。**
 *
 */
private interface Command {
    /**
     * コマンドを実行します。
     *
     * @return コマンドの実行結果
     */
    fun execute(): Any?

    /**
     * 有効なKARTEのコマンドかどうか検証します。
     *
     * @return コマンドの検証結果
     */
    fun validate(uri: Uri): Boolean {
        val scheme = uri.scheme
        if (scheme != null && scheme.startsWith("krt-") && scheme.length == karteSchemeLength) {
            return true
        }
        return false
    }
}

private class OpenSettingsCommand: Command {
    override fun execute(): Any? {
        val uriString = "package:" + KarteApp.self.application.packageName
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse(uriString))
    }

    override fun validate(uri: Uri): Boolean {
        val scheme = uri.scheme

        // Handle legacy scheme.
        if (scheme != null && scheme == "app-settings") {
            return true
        }
        if (super.validate(uri) && uri.host == "open-settings" ) {
            return true
        }
        return false
    }
}

private class OpenStoreCommand: Command {
    override fun execute(): Any? {
        val uriString = "market://details?id=" + KarteApp.self.application.packageName
        return Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
    }

    override fun validate(uri: Uri): Boolean {
        if (super.validate(uri) && uri.host == "open-store" ) {
            return true
        }
        return false
    }
}