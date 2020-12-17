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
package io.karte.android.modules.deeplinkevent

import android.content.Intent
import io.karte.android.BuildConfig
import io.karte.android.KarteApp
import io.karte.android.core.library.DeepLinkModule
import io.karte.android.core.library.Library
import io.karte.android.tracking.Event
import io.karte.android.tracking.EventName
import io.karte.android.tracking.Tracker

private const val LOG_TAG = "Karte.DeepLinkEvent"
private const val SENT_FLAG = "_krt_deep_link_event"

internal class DeepLinkEvent : Library, DeepLinkModule {

    //region Library
    override val name: String = "DeepLinkEvent"
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
        if (intent == null || intent.data == null) return
        if (intent.hasExtra(SENT_FLAG)) return
        Tracker.track(DeepLinkAppEvent(DeepLinkEventName.AppOpen, intent.data.toString()))
        intent.putExtra(SENT_FLAG, true)
    }
    //endregion
}

private class DeepLinkAppEvent(eventName: DeepLinkEventName, url: String) :
    Event(eventName, values = mapOf("url" to url))

private enum class DeepLinkEventName(override val value: String) : EventName {
    AppOpen("deep_link_app_open"),
}
