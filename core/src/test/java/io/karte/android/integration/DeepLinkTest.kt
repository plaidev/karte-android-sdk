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
import io.karte.android.KarteApp
import io.karte.android.TrackerRequestDispatcher
import io.karte.android.TrackerTestCase
import io.karte.android.parseBody
import io.karte.android.proceedBufferedCall
import io.karte.android.utilities.map
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.android.controller.ActivityController

class NewIntentActivity : Activity() {
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        KarteApp.onNewIntent(intent)
    }
}

abstract class DeepLinkTestCase : TrackerTestCase() {
    private lateinit var dispatcher: TrackerRequestDispatcher
    var activityController: ActivityController<*>? = null

    @Before
    fun setup() {
        dispatcher = TrackerRequestDispatcher()
        server.dispatcher = dispatcher
    }

    @After
    fun teardown() {
        activityController?.destroy()
        activityController = null
    }

    fun launchByDeepLink(uriString: String?) {
        launchByDeepLink(uriString, Activity::class.java)
    }

    fun <T : Activity> launchByDeepLink(uriString: String?, activityClass: Class<T>) {
        val intent = if (uriString == null) {
            Intent(Intent.ACTION_VIEW)
        } else {
            Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
        }
        activityController = Robolectric.buildActivity(activityClass, intent)
            .create().start().resume()
    }

    fun relaunchWithNewIntent(uriString: String) {
        activityController?.newIntent(Intent(Intent.ACTION_VIEW, Uri.parse(uriString)))
            ?.start()?.resume()
    }

    private fun getEvents(eventNameFilter: String? = null): List<JSONObject> {
        var events = JSONObject(server.takeRequest().parseBody())
            .getJSONArray("events")
            .map { it }
            .filterIsInstance<JSONObject>()
        eventNameFilter?.let {
            events = events.filter { it.getString("event_name") == eventNameFilter }
        }
        return events
    }

    abstract val eventName: String

    fun assertEventOccurred(key: String, value: String) {
        val events = getEvents(eventName)
        assertThat(events).hasSize(1)

        val event = events[0]
        assertThat(event.getString("event_name")).isEqualTo(eventName)
        assertThat(event.getJSONObject("values").getString(key)).isEqualTo(value)
    }

    fun assertNoEvent() {
        assertThat(getEvents(eventName)).isEmpty()
    }
}

class FindMySelf : DeepLinkTestCase() {
    override val eventName = "native_find_myself"

    @Test
    fun FindMySelfが1回のみ発生() {
        launchByDeepLink("test://karte.io/find_myself?src=qr")
        proceedBufferedCall()

        assertEventOccurred("src", "qr")
    }

    @Test
    fun 再起動すればFindMySelfが繰り返し発生() {
        launchByDeepLink("test://karte.io/find_myself?src=qr")
        proceedBufferedCall()

        assertEventOccurred("src", "qr")

        activityController?.pause()?.stop()?.destroy()
        launchByDeepLink("test://karte.io/find_myself?src=qr2")
        proceedBufferedCall()

        assertEventOccurred("src", "qr2")
    }

    @Test
    fun 実装済みならonNewIntentでも発生() {
        launchByDeepLink("test://karte.io/find_myself?src=qr", NewIntentActivity::class.java)
        proceedBufferedCall()

        assertEventOccurred("src", "qr")

        activityController?.pause()?.stop()
        relaunchWithNewIntent("test://karte.io/find_myself?src=qr2")
        proceedBufferedCall()

        assertEventOccurred("src", "qr2")
    }

    @Test
    fun 未実装ならonNewIntentでは発生しない() {
        launchByDeepLink("test://karte.io/find_myself?src=qr")
        proceedBufferedCall()

        assertEventOccurred("src", "qr")

        activityController?.pause()?.stop()
        relaunchWithNewIntent("test://karte.io/find_myself?src=qr2")
        proceedBufferedCall()

        assertNoEvent()
    }

    @Test
    fun queryがなければ発生しない() {
        launchByDeepLink("test://karte.io/find_myself")
        proceedBufferedCall()

        assertNoEvent()
    }

    @Test
    fun hostが違えば発生しない() {
        launchByDeepLink("test://plaid.co.jp/find_myself?src=qr")
        proceedBufferedCall()

        assertNoEvent()
    }

    @Test
    fun パスが違えば発生しない() {
        launchByDeepLink("test://karte.io/preview?src=qr")
        proceedBufferedCall()

        assertNoEvent()
    }
}

class DeepLinkEventTest : DeepLinkTestCase() {
    override val eventName = "deep_link_app_open"

    @Test
    fun DeepLinkAppOpenが1回のみ発生() {
        val url = "test://anyrequest?test=true"
        launchByDeepLink(url)
        proceedBufferedCall()

        assertEventOccurred("url", url)
    }

    @Test
    fun 再起動すればFindMySelfが繰り返し発生() {
        val url = "test://anyrequest?test=true"
        launchByDeepLink(url)
        proceedBufferedCall()

        assertEventOccurred("url", url)

        activityController?.pause()?.stop()?.destroy()
        val url2 = "test://anotherrequest?test=true"
        launchByDeepLink(url2)
        proceedBufferedCall()

        assertEventOccurred("url", url2)
    }

    @Test
    fun 実装済みならonNewIntentでも発生() {
        val url = "test://anyrequest?test=true"
        launchByDeepLink(url, NewIntentActivity::class.java)
        proceedBufferedCall()

        assertEventOccurred("url", url)

        activityController?.pause()?.stop()
        val url2 = "test://anotherrequest?test=true"
        relaunchWithNewIntent(url2)
        proceedBufferedCall()

        assertEventOccurred("url", url2)
    }

    @Test
    fun 未実装ならonNewIntentでは発生しない() {
        val url = "test://anyrequest?test=true"
        launchByDeepLink(url)
        proceedBufferedCall()

        assertEventOccurred("url", url)

        activityController?.pause()?.stop()
        val url2 = "test://anotherrequest?test=true"
        relaunchWithNewIntent(url2)
        proceedBufferedCall()

        assertNoEvent()
    }

    @Test
    fun URIが空文字なら発生する() {
        launchByDeepLink("")
        proceedBufferedCall()

        assertEventOccurred("url", "")
    }

    @Test
    fun URIがなければ発生しない() {
        launchByDeepLink(null)
        proceedBufferedCall()

        assertNoEvent()
    }
}
