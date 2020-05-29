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
package io.karte.android

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Correspondence
import io.karte.android.core.config.Config
import io.karte.android.tracking.queue.THREAD_NAME
import io.karte.android.utilities.gunzip
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONArray
import org.json.JSONObject
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowLooper

fun setupKarteApp(
    server: MockWebServer,
    appKey: String,
    configBuilder: Config.Builder = Config.Builder()
): KarteApp {
    KarteApp.setup(
        application(),
        appKey,
        configBuilder.baseUrl(server.url("/native").toString()).build()
    )
    return KarteApp.self
}

fun tearDownKarteApp() {
    KarteApp.self.teardown()
}

fun application(): Application {
    return ApplicationProvider.getApplicationContext()
}

fun getThreadByName(threadName: String = THREAD_NAME): Thread? {
    return Thread.getAllStackTraces().keys.firstOrNull { it.name == threadName }
}

fun proceedBufferedCall() {
    println("proceedBufferedCall")
    val scheduler =
        Shadows.shadowOf(ShadowLooper.getLooperForThread(getThreadByName(THREAD_NAME)))
            .scheduler
    // Schedulerのbuffer計算バグで一度目のループで実行されないケースがあるため2回呼ぶ
    while (scheduler.advanceToNextPostedRunnable());
    while (scheduler.advanceToNextPostedRunnable());
    Robolectric.flushForegroundThreadScheduler()
}

fun createMessage(
    campaignId: String = "sample_campaign",
    shortenId: String = "sample_shorten",
    content: JSONObject = JSONObject(),
    pluginType: String = "webpopup"
): JSONObject {
    val action = JSONObject()
        .put("campaign_id", campaignId)
        .put("shorten_id", shortenId)
        .put("plugin_type", pluginType)
        .put("content", content)
    val campaign = JSONObject()
        .put("_id", campaignId)
        .put("service_action_type", pluginType)

    return JSONObject().put("action", action).put("campaign", campaign)
}

fun createControlGroupMessage(
    campaignId: String = "sample_campaign",
    shortenId: String = "__sample_shorten",
    serviceActionType: String = "webpopup"
): JSONObject {
    val action = JSONObject()
        .put("shorten_id", shortenId)
        .put("type", "control")
    val campaign = JSONObject()
        .put("_id", campaignId)
        .put("service_action_type", serviceActionType)

    return JSONObject().put("action", action).put("campaign", campaign)
}

fun createMessageOpen(
    campaignId: String = "sample_campaign",
    shortenId: String = "sample_shorten"
): JSONObject = JSONObject()
    .put("event_name", "message_open")
    .put(
        "values", JSONObject()
            .put(
                "message", JSONObject()
                    .put("campaign_id", campaignId).put("shorten_id", shortenId)
            )
    )

fun createMessagesResponse(messages: JSONArray): JSONObject {
    return JSONObject().put("response", JSONObject().put("messages", messages))
}

fun createMessageResponse(message: JSONObject): JSONObject {
    return createMessagesResponse(JSONArray().put(message))
}

val eventNameTransform: Correspondence<JSONObject, String> =
    Correspondence.transforming<JSONObject, String>(
        { input -> input?.getString("event_name") },
        "Get event name"
    )

fun JSONArray.toList(): List<JSONObject> {
    val list = mutableListOf<JSONObject>()
    for (i in 0 until length()) {
        list.add(getJSONObject(i))
    }
    return list
}

fun Array<out JSONObject>.toJSONArray(): JSONArray {
    val ret = JSONArray()
    for (json in this) {
        ret.put(json)
    }
    return ret
}

fun RecordedRequest.parseBody(): String {
    return gunzip(this.body.clone().readByteArray())!!
}
