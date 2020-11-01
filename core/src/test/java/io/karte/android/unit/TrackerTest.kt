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
package io.karte.android.unit

import io.karte.android.KarteApp
import io.karte.android.core.logger.LogLevel
import io.karte.android.proceedBufferedCall
import io.karte.android.setupKarteApp
import io.karte.android.tearDownKarteApp
import io.karte.android.tracking.Tracker
import io.karte.android.utilities.connectivity.Connectivity
import io.karte.android.utilities.http.Client
import io.karte.android.utilities.http.Request
import io.karte.android.utilities.http.Response
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TrackerTest {
    private val dummyResponse = Response(200, hashMapOf(), "{response:{}}")

    @Before
    fun init() {
        KarteApp.setLogLevel(LogLevel.VERBOSE)
        setupKarteApp()

        mockkObject(Client, Connectivity)
        every { Client.execute(any()) } returns dummyResponse
        every { Connectivity.isOnline(any()) } returns true
    }

    @After
    fun tearDown() {
        tearDownKarteApp()
        unmockkObject(Client, Connectivity)
    }

    private fun captureRequest(): CapturingSlot<Request<String>> {
        val slot = slot<Request<String>>()
        every { Client.execute(capture(slot)) } returns dummyResponse
        proceedBufferedCall()
        return slot
    }

    @Test
    fun trackShouldCopyAndFormatParameter() {
        val jsonparam = JSONObject().put("item_name", "t-shirt")
        val dateParam = Date()
        val arrayParam = JSONArray().put(1).put("hoge")
        Tracker.track(
            "buy",
            mapOf("jsonparam" to jsonparam, "dateparam" to dateParam, "arrayparam" to arrayParam)
        )
        val slot = captureRequest()

        val sentEventValues =
            JSONObject(slot.captured.body as String).getJSONArray("events").getJSONObject(0)
                .getJSONObject("values")
        Assert.assertTrue(jsonparam.toString().equals(sentEventValues.get("jsonparam").toString()))
        Assert.assertEquals(dateParam.time / 1000, sentEventValues.getLong("dateparam"))
        Assert.assertEquals(
            arrayParam.toString(),
            sentEventValues.getJSONArray("arrayparam").toString()
        )
    }

    @Test
    fun noValueInJsonObject() {
        Tracker.track("buy", JSONObject().put("param", "value").put("no_param", null))
        val slot = captureRequest()

        val sentEventValues =
            JSONObject(slot.captured.body as String).getJSONArray("events").getJSONObject(0)
                .getJSONObject("values")
        Assert.assertTrue(sentEventValues.has("param"))
        Assert.assertFalse(sentEventValues.has("no_param"))
    }

    @Test
    fun noValueInJsonArray() {
        Tracker.track("buy", mapOf("array" to listOf("value", null)))
        val slot = captureRequest()

        val sentEventValues =
            JSONObject(slot.captured.body as String).getJSONArray("events").getJSONObject(0)
                .getJSONObject("values")
        Assert.assertFalse(sentEventValues.getJSONArray("array").isNull(0))
        Assert.assertTrue(sentEventValues.getJSONArray("array").isNull(1))
    }

    @Test
    fun noUncaughtExceptionOnNetworkError() {
        every { Client.execute(any()) } throws IOException("network error")
        Tracker.track("buy", mapOf("item_name" to "t-shirt"))
        proceedBufferedCall()
    }

    @Test
    fun noUncaughtExceptionOnHttpError() {
        every { Client.execute(any()) } returns Response(400, hashMapOf(), "{messages:[]}")
        Tracker.track("buy", mapOf("item_name" to "t-shirt"))
        proceedBufferedCall()
    }
}
