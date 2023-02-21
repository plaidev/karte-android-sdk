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
package io.karte.android.inappmessaging.unit

import io.karte.android.KarteApp
import io.karte.android.core.config.Config
import io.karte.android.inappmessaging.internal.MessageModel
import io.karte.android.test_lib.createControlGroupMessage
import io.karte.android.test_lib.createMessage
import io.karte.android.tracking.AppInfo
import io.karte.android.tracking.CustomEventName
import io.karte.android.tracking.Event
import io.karte.android.tracking.client.TrackRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.json.JSONArray
import org.json.JSONObject

internal fun createMessagePopup(campaignId: String = "sample_campaign"): MessageModel {
    val req =
        TrackRequest("", "", "", "", listOf(Event(CustomEventName("viewMock"), values = null)))
    val res1 = JSONObject()
        .put("messages", JSONArray().put(createMessage(campaignId = campaignId)))
    return MessageModel(res1, req)
}

internal fun createMessageRemoteConfig(): MessageModel {
    val req =
        TrackRequest("", "", "", "", listOf(Event(CustomEventName("viewMock"), values = null)))
    val res1 = JSONObject()
        .put("messages", JSONArray().put(createMessage(pluginType = "remote_config")))
    return MessageModel(res1, req)
}

internal fun createMessageControlGroup(): MessageModel {
    val req =
        TrackRequest("", "", "", "", listOf(Event(CustomEventName("viewMock"), values = null)))
    val res1 = JSONObject()
        .put("messages", JSONArray().put(createControlGroupMessage()))
    return MessageModel(res1, req)
}

internal fun mockKarteApp(): KarteApp {
    mockkObject(KarteApp.Companion)
    every { KarteApp.visitorId } returns "visitor"

    val app = mockk<KarteApp>()
    val appInfo = mockk<AppInfo>()
    val config = mockk<Config>()
    every { app.appInfo } returns appInfo
    every { app.config } returns config
    every { app.appKey } returns "appkey"
    every { appInfo.json } returns JSONObject().put("version_name", 1)
    every { config.baseUrl } returns "https://myurl:8080"
    return app
}

internal fun unmockKarteApp() {
    unmockkObject(KarteApp.Companion)
}
