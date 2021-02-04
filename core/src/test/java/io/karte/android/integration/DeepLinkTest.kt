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
package io.karte.android.integration

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.google.common.truth.Truth.assertThat
import io.karte.android.TrackerRequestDispatcher
import io.karte.android.TrackerTestCase
import io.karte.android.parseBody
import io.karte.android.proceedBufferedCall
import io.karte.android.utilities.map
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric

abstract class DeepLinkTestCase : TrackerTestCase() {
    private lateinit var dispatcher: TrackerRequestDispatcher

    @Before
    fun setup() {
        dispatcher = TrackerRequestDispatcher()
        server.dispatcher = dispatcher
    }

    fun launchByDeepLink(uriString: String) {
        Robolectric.buildActivity(
            Activity::class.java,
            Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
        ).create().start().resume()
    }

    fun getEvents(eventNameFilter: String? = null): List<JSONObject> {
        var events = JSONObject(server.takeRequest().parseBody())
            .getJSONArray("events")
            .map { it }
            .filterIsInstance<JSONObject>()
        eventNameFilter?.let {
            events = events.filter { it.getString("event_name") == eventNameFilter }
        }
        return events
    }
}

class FindMySelf : DeepLinkTestCase() {
    private val EVENT_NAME = "native_find_myself"

    @Test
    fun FindMySelfが1回のみ発生() {
        launchByDeepLink("test://karte.io/find_myself?src=qr")
        proceedBufferedCall()

        val events = getEvents(EVENT_NAME)
        assertThat(events).hasSize(1)

        val event = events[0]
        assertThat(event.getString("event_name")).isEqualTo(EVENT_NAME)
        assertThat(event.getJSONObject("values").getString("src")).isEqualTo("qr")
    }

    @Test
    fun queryがなければ発生しない() {
        launchByDeepLink("test://karte.io/find_myself")
        proceedBufferedCall()

        assertThat(getEvents(EVENT_NAME)).isEmpty()
    }

    @Test
    fun hostが違えば発生しない() {
        launchByDeepLink("test://plaid.co.jp/find_myself?src=qr")
        proceedBufferedCall()

        assertThat(getEvents(EVENT_NAME)).isEmpty()
    }

    @Test
    fun パスが違えば発生しない() {
        launchByDeepLink("test://karte.io/preview?src=qr")
        proceedBufferedCall()

        assertThat(getEvents(EVENT_NAME)).isEmpty()
    }
}

class DeepLinkEventTest : DeepLinkTestCase() {
    private val EVENT_NAME = "deep_link_app_open"

    @Test
    fun DeepLinkAppOpenが1回のみ発生() {
        val url = "test://anyrequest?test=true"
        launchByDeepLink(url)
        proceedBufferedCall()

        val events = getEvents(EVENT_NAME)
        assertThat(events).hasSize(1)

        val event = events[0]
        assertThat(event.getString("event_name")).isEqualTo(EVENT_NAME)
        assertThat(event.getJSONObject("values").getString("url")).isEqualTo(url)
    }

    @Test
    fun URIがなければ発生しない() {
        Robolectric.buildActivity(Activity::class.java, Intent(Intent.ACTION_VIEW)).create().start()
            .resume()
        proceedBufferedCall()

        assertThat(getEvents(EVENT_NAME)).isEmpty()
    }
}
