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
package io.karte.android.modules.findmyself

import android.content.Intent
import android.net.Uri
import io.karte.android.BuildConfig
import io.karte.android.KarteApp
import io.karte.android.core.library.DeepLinkModule
import io.karte.android.core.library.Library
import io.karte.android.core.logger.Logger
import io.karte.android.tracking.Event
import io.karte.android.tracking.EventName
import io.karte.android.tracking.Tracker
import io.karte.android.tracking.Values
import io.karte.android.utilities.filterNotNull

private const val LOG_TAG = "Karte.FindMySelf"

internal class FindMyself : Library, DeepLinkModule {

    //region Library
    override val name: String = FindMyself::class.java.simpleName
    override val version: String = BuildConfig.VERSION_NAME
    override val isPublic: Boolean = false

    override fun configure(app: KarteApp) {
        app.register(this)
    }

    override fun unconfigure(app: KarteApp) {
        app.unregister(this)
    }
    //endregion

    //region DeepLinkModule
    override fun handle(intent: Intent?) {
        val uri = intent?.data
        if (uri == null || uri.host != "karte.io" || uri.path != "/find_myself") return
        Logger.d(LOG_TAG, "handle $uri")
        val values =
            uri.queryParameterNames
                .associateWith { uri.getQueryParameter(it) }
                .filterNotNull()
        if (values.isEmpty()) return
        if (values.containsKey("sent")) {
            Logger.d(LOG_TAG, "Event already sent.")
            return
        }
        val newUri =
            Uri.Builder().scheme(uri.scheme).authority(uri.authority).encodedQuery(uri.encodedQuery)
                .appendQueryParameter("sent", "true").build()

        Logger.d(LOG_TAG, "Sending FindMySelf event")
        Tracker.track(FindMySelfEvent(values))
        // override with sent flag.
        intent.data = newUri
    }
    //endregion
}

private class FindMySelfEvent(values: Values) : Event(FindMySelfEventName(), values)

private class FindMySelfEventName(override val value: String = "native_find_myself") : EventName
