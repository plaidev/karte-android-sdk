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

import android.webkit.WebView
import com.google.common.truth.Truth.assertThat
import io.karte.android.KarteApp
import io.karte.android.core.logger.LogLevel
import io.karte.android.core.usersync.UserSync
import io.karte.android.test_lib.integration.DryRunTestCase
import io.karte.android.tracking.Event
import io.karte.android.tracking.Tracker
import io.karte.android.tracking.TrackerDelegate
import org.json.JSONObject
import org.junit.Test

class DryRunTest : DryRunTestCase() {
    @Test
    fun testKarteApp() {
        assertThat(KarteApp.isOptOut).isFalse()
        assertThat(KarteApp.visitorId).isEmpty()

        KarteApp.setLogLevel(LogLevel.VERBOSE)
        KarteApp.optIn()
        KarteApp.optOut()
        KarteApp.renewVisitorId()
        assertDryRun()
    }

    @Test
    fun testTracker() {
        Tracker.setDelegate(object : TrackerDelegate {
            override fun intercept(event: Event): Event {
                return event
            }
        })
        Tracker.identify("dummy")
        Tracker.view("test")
        Tracker.track("test")
        Tracker.attribute(JSONObject())

        assertDryRun()
    }

    @Test
    fun testUserSync() {
        @Suppress("DEPRECATION")
        assertThat(UserSync.appendUserSyncQueryParameter("test")).isEqualTo("test")
        assertThat(UserSync.getUserSyncScript()).isNull()

        UserSync.setUserSyncScript(WebView(application))
        assertDryRun()
    }
}
