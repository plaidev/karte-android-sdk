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

package io.karte.android.modules.commands

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import io.karte.android.BuildConfig
import io.karte.android.KarteApp
import io.karte.android.core.library.CommandModule
import io.karte.android.core.library.Library

internal class CoreCommands : Library {
    override val name: String = "CoreCommands"
    override val version: String = BuildConfig.LIB_VERSION
    override val isPublic: Boolean = false
    private val commands = listOf(OpenSettingsCommandExecutor(), OpenStoreCommandExecutor())

    override fun configure(app: KarteApp) {
        commands.forEach { app.register(it) }
    }

    override fun unconfigure(app: KarteApp) {
        commands.forEach { app.unregister(it) }
    }
}

private class OpenSettingsCommandExecutor : CommandModule {
    override val name: String = "OpenSettingsCommand"

    override fun execute(uri: Uri, isDelay: Boolean): Intent {
        val uriString = "package:" + KarteApp.self.application.packageName
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse(uriString))
    }

    override fun validate(uri: Uri): Boolean {
        val scheme = uri.scheme

        // Handle legacy scheme.
        if (scheme != null && scheme == "app-settings") {
            return true
        }
        if (super.validate(uri) && uri.host == "open-settings") {
            return true
        }
        return false
    }
}

private class OpenStoreCommandExecutor : CommandModule {
    override val name: String = "OpenStoreCommand"
    override fun execute(uri: Uri, isDelay: Boolean): Intent {
        val uriString = "market://details?id=" + KarteApp.self.application.packageName
        return Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
    }

    override fun validate(uri: Uri): Boolean {
        if (super.validate(uri) && uri.host == "open-store") {
            return true
        }
        return false
    }
}
