//
//  Copyright 2026 PLAID, Inc.
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
package io.karte.android.unit.nativesdkconfig

import com.google.common.truth.Truth.assertThat
import io.karte.android.core.nativesdkconfig.NativeSDKConfigRequest
import io.karte.android.utilities.http.HEADER_APP_KEY
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class NativeSDKConfigRequestTest {

    private val cdnBaseUrl = "https://cdn.example.com"
    private val appKey = "testappkey_1234567890123456789"

    @Test
    fun testNativeSDKConfigRequestConstruction() {
        val req = NativeSDKConfigRequest(cdnBaseUrl, appKey)

        assertThat(req.method).isEqualTo("GET")
        assertThat(req.url).isEqualTo("$cdnBaseUrl/v0/native/sdk-config?app_key=$appKey")
        assertThat(req.headers).containsEntry(HEADER_APP_KEY, appKey)
        assertThat(req.hasBody).isFalse()
    }
}
