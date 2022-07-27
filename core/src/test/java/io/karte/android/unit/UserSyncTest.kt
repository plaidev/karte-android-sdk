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

import android.app.Activity
import android.net.Uri
import android.util.Base64
import android.webkit.WebView
import io.karte.android.KarteApp
import io.karte.android.RobolectricTestCase
import io.karte.android.core.usersync.UserSync
import io.karte.android.setupKarteApp
import io.karte.android.shadow.customShadowOf
import io.karte.android.tearDownKarteApp
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import org.json.JSONObject
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric
import java.util.Date

class UserSyncTest : RobolectricTestCase() {
    private lateinit var webView: WebView

    private val expected
        get() = JSONObject()
            .put("visitor_id", KarteApp.visitorId)
            .put("app_info", KarteApp.self.appInfo?.json)
            .put("ts", Date().time / 1000)
            .toString()

    @Before
    fun init() {
        webView = WebView(Robolectric.buildActivity(Activity::class.java).start().get())

        mockkConstructor(Date::class)
        every { anyConstructed<Date>().time } returns 1561971767890

        setupKarteApp()
    }

    @After
    fun tearDown() {
        tearDownKarteApp()
        unmockkConstructor(Date::class)
    }

    @Test
    fun appendUserSyncQueryParameter_String() {
        @Suppress("DEPRECATION")
        val actual = UserSync.appendUserSyncQueryParameter("https://plaid.co.jp?hoge=fuga")
        val base64EncodedString = Uri.parse(actual).getQueryParameter("_k_ntvsync_b")
        val string = String(Base64.decode(base64EncodedString, Base64.NO_WRAP))
        val param = JSONObject(string)
        Assert.assertEquals(expected, param.toString())
    }

    @Test
    fun appendUserSyncQueryParameter_Uri() {
        @Suppress("DEPRECATION")
        val actual =
            UserSync.appendUserSyncQueryParameter(Uri.parse("https://plaid.co.jp?hoge=fuga"))
        val base64EncodedString = Uri.parse(actual).getQueryParameter("_k_ntvsync_b")
        val string = String(Base64.decode(base64EncodedString, Base64.NO_WRAP))
        val param = JSONObject(string)
        Assert.assertEquals(expected, param.toString())
    }

    @Test
    fun setUserSyncScript() {
        val expected = String.format("window.__karte_ntvsync = %s;", expected)

        UserSync.setUserSyncScript(webView)
        val actual = customShadowOf(webView).lastEvaluatedJavascript
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun getUserSyncScript() {
        val expected = String.format("window.__karte_ntvsync = %s;", expected)

        val actual = UserSync.getUserSyncScript()
        Assert.assertEquals(expected, actual)
    }
}
