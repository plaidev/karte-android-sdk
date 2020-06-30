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

import android.content.Intent
import android.net.Uri
import io.karte.android.KarteApp
import io.karte.android.RobolectricTestCase
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CommandTest: RobolectricTestCase() {
    private val testApiKey = "aWgSxztqGSpKN0otg5w1ruUk8kTBF5vb"

    @Before
    fun setup() {
        KarteApp.setup(application, "hoge")
    }

    @Test
    fun testCommand() {
        testCommandHasValidIntent(Uri.parse("app-settings:"), "package:io.karte.android.core")
        testCommandHasValidIntent(Uri.parse("krt-$testApiKey://open-settings?key=value"), "package:io.karte.android.core")
        testCommandHasValidIntent(Uri.parse("krt-$testApiKey://open-store"), "market://details?id=io.karte.android.core")

        testCommandReturnsNull(Uri.parse("invalid-scheme:"))
        testCommandReturnsNull(Uri.parse("app-settings-invalid:"))
        testCommandReturnsNull(Uri.parse("krt-$testApiKey://open-settings-invalid"))
    }

    private fun testCommandHasValidIntent(uri: Uri, expected: String?) {
        val intent = KarteApp?.self?.executeCommand(uri)?.filterIsInstance<Intent>()
            ?.firstOrNull()
        Assert.assertEquals(expected, intent?.data.toString())
    }

    private fun testCommandReturnsNull(uri: Uri) {
        val intent = KarteApp?.self?.executeCommand(uri)?.filterIsInstance<Intent>()?.firstOrNull()
        Assert.assertNull(intent)
    }
}