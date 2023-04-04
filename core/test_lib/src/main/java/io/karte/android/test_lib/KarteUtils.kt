//
//  Copyright 2023 PLAID, Inc.
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
package io.karte.android.test_lib

import io.karte.android.KarteApp
import io.karte.android.core.config.Config
import okhttp3.mockwebserver.MockWebServer

internal const val VALID_APPKEY = "sampleappkey_1234567890123456789"

fun setupKarteApp(
    server: MockWebServer? = null,
    configBuilder: Config.Builder = Config.Builder(),
    appKey: String = VALID_APPKEY
): KarteApp {
    val config = if (server != null) {
        configBuilder.baseUrl(server.url("/native").toString()).build()
    } else {
        configBuilder.build()
    }
    KarteApp.setup(application(), appKey, config)
    return InternalUtils.karteApp
}

fun tearDownKarteApp() {
    InternalUtils.teardownKarteApp()
}
