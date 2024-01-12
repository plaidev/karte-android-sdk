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

package io.karte.android.inappmessaging.unit

import com.google.common.truth.Truth.assertThat
import io.karte.android.inappmessaging.InAppMessagingConfig
import io.karte.android.test_lib.application
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConfigTest {
    @Test
    fun default_config() {
        val config = InAppMessagingConfig.build()

        // 未設定時は空文字, 内部で使用前に補完する
        assertThat(config.overlayBaseUrl).isEqualTo("https://cf-native.karte.io")
    }

    @Test
    fun config_from_resource() {
        // 全てresourceから読み込む場合
        val config = InAppMessagingConfig.fillFromResource(application(), null)

        assertThat(config.overlayBaseUrl).isEqualTo("https://cf-native-rs.karte.io")
    }

    @Test
    fun customize_config() {
        val config = InAppMessagingConfig.build {
            overlayBaseUrl = "https://cf-native-js.karte.io"
        }

        assertThat(config.overlayBaseUrl).isEqualTo("https://cf-native-js.karte.io")
    }

    @Test
    fun config_from_resource_with_custom() {
        // code優先
        InAppMessagingConfig.fillFromResource(application(), InAppMessagingConfig.build {
            overlayBaseUrl = "https://cf-native-js.karte.io"
        }).let { config ->
            assertThat(config.overlayBaseUrl).isEqualTo("https://cf-native-js.karte.io")
        }
        // default値を指定してもresourceに上書きされない
        InAppMessagingConfig.fillFromResource(application(), InAppMessagingConfig.build {
            overlayBaseUrl = "https://cf-native.karte.io"
        }).let { config ->
            assertThat(config.overlayBaseUrl).isEqualTo("https://cf-native.karte.io")
        }
    }
}
