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

package io.karte.android.unit

import com.google.common.truth.Truth.assertThat
import io.karte.android.core.config.Config
import io.karte.android.core.library.LibraryConfig
import io.karte.android.test_lib.application
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

class DummyLibraryConfig(val name: String) : LibraryConfig

@RunWith(RobolectricTestRunner::class)
class ConfigTest {
    @Test
    fun default_config() {
        val config = Config.build()

        assertThat(config.apiKey).isEmpty()
        assertThat(config.appKey).isEmpty()
        assertThat(config.isValidAppKey).isFalse()
        assertThat(config.baseUrl).isEqualTo("https://b.karte.io/v0/native")
        assertThat(config.dataLocation).isEqualTo("tw")
        assertThat(config.isDryRun).isFalse()
        assertThat(config.isOptOut).isFalse()
        assertThat(config.enabledTrackingAaid).isFalse()
        assertThat(config.libraryConfigs).isEmpty()
    }

    @Test
    fun config_from_resource() {
        // 全てresourceから読み込む場合
        val config = Config.fillFromResource(application(), null)

        assertThat(config.apiKey).isEqualTo("sampleapikey_1234567890123456789")
        assertThat(config.appKey).isEqualTo("sampleappkey_1234567890123456789")
        assertThat(config.isValidAppKey).isTrue()
        assertThat(config.baseUrl).isEqualTo("https://b-rs.karte.io/v0/native")
        assertThat(config.dataLocation).isEqualTo("rs")
        assertThat(config.isDryRun).isFalse()
        assertThat(config.isOptOut).isFalse()
        assertThat(config.enabledTrackingAaid).isFalse()
        assertThat(config.libraryConfigs).isEmpty()
    }

    @Test
    fun customize_config() {
        val config = Config.build {
            apiKey = "dummy_api_key"
            appKey = "dummy_application_key_1234567890"
            baseUrl = "https://b-jp.karte.io"
            dataLocation = "jp"
            isDryRun = true
            isOptOut = true
            enabledTrackingAaid = true
            libraryConfigs = listOf(DummyLibraryConfig("test"))
        }

        assertThat(config.apiKey).isEqualTo("dummy_api_key")
        assertThat(config.appKey).isEqualTo("dummy_application_key_1234567890")
        assertThat(config.isValidAppKey).isTrue()
        assertThat(config.baseUrl).isEqualTo("https://b-jp.karte.io/v0/native")
        assertThat(config.dataLocation).isEqualTo("jp")
        assertThat(config.isDryRun).isTrue()
        assertThat(config.isOptOut).isTrue()
        assertThat(config.enabledTrackingAaid).isTrue()
        assertThat(config.libraryConfigs).isNotEmpty()
    }

    @Test
    fun config_from_resource_or_code() {
        fun assertFromResource(config: Config, vararg targets: String) {
            if (targets.contains("appKey"))
                assertThat(config.appKey).isEqualTo("sampleappkey_1234567890123456789")
            if (targets.contains("apiKey"))
                assertThat(config.apiKey).isEqualTo("sampleapikey_1234567890123456789")
            if (targets.contains("baseUrl"))
                assertThat(config.baseUrl).isEqualTo("https://b-rs.karte.io/v0/native")
            if (targets.contains("dataLocation"))
                assertThat(config.dataLocation).isEqualTo("rs")
        }

        fun assertFromCode(config: Config, vararg targets: String) {
            if (targets.contains("appKey"))
                assertThat(config.appKey).isEqualTo("dummy_application_key_1234567890")
            if (targets.contains("apiKey"))
                assertThat(config.apiKey).isEqualTo("dummy_api_key")
            if (targets.contains("baseUrl"))
                assertThat(config.baseUrl).isEqualTo("https://b-jp.karte.io/v0/native")
            if (targets.contains("dataLocation"))
                assertThat(config.dataLocation).isEqualTo("jp")
        }

        // appKey
        Config.fillFromResource(application(), Config.build {
            appKey = "dummy_application_key_1234567890"
        }).let { config ->
            assertFromCode(config, "appKey")
            assertFromResource(config, "apiKey", "baseUrl", "dataLocation")
        }
        // 空文字は上書き
        Config.fillFromResource(application(), Config.build {
            appKey = ""
        }).let { config ->
            assertFromResource(config, "appKey", "apiKey", "baseUrl", "dataLocation")
        }

        // apiKey
        Config.fillFromResource(application(), Config.build {
            apiKey = "dummy_api_key"
        }).let { config ->
            assertFromCode(config, "apiKey")
            assertFromResource(config, "appKey", "baseUrl", "dataLocation")
        }
        // 空文字は上書き
        Config.fillFromResource(application(), Config.build {
            apiKey = ""
        }).let { config ->
            assertFromResource(config, "appKey", "apiKey", "baseUrl", "dataLocation")
        }

        // baseUrl
        Config.fillFromResource(application(), Config.build {
            baseUrl = "https://b-jp.karte.io"
        }).let { config ->
            assertFromCode(config, "baseUrl")
            assertFromResource(config, "appKey", "apiKey", "dataLocation")
        }
        // baseUrlにdefault値を指定しても、resourceで上書きされない
        Config.fillFromResource(application(), Config.build {
            baseUrl = "https://b.karte.io"
        }).let { config ->
            assertThat(config.baseUrl).isEqualTo("https://b.karte.io/v0/native")
            assertFromResource(config, "appKey", "apiKey", "dataLocation")
        }
        // 空文字は上書き
        Config.fillFromResource(application(), Config.build {
            baseUrl = ""
        }).let { config ->
            assertFromResource(config, "appKey", "apiKey", "baseUrl", "dataLocation")
        }

        // dataLocation
        Config.fillFromResource(application(), Config.build {
            dataLocation = "jp"
        }).let { config ->
            assertFromCode(config, "dataLocation")
            assertFromResource(config, "appKey", "apiKey", "baseUrl")
        }
        // 空文字は上書き
        Config.fillFromResource(application(), Config.build {
            dataLocation = ""
        }).let { config ->
            assertFromResource(config, "appKey", "apiKey", "baseUrl", "dataLocation")
        }
    }
}
