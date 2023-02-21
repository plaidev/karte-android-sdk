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
package io.karte.android.test_lib.integration

import com.google.common.truth.Truth
import io.karte.android.core.config.Config
import io.karte.android.test_lib.setupKarteApp
import org.junit.Before

abstract class DryRunTestCase : SetupTestCase() {
    @Before
    fun setup() {
        setupKarteApp(server, Config.Builder().isDryRun(true))
    }

    /**Queue用のスレッドが生成されていないか、serverにリクエストが飛んでないか確認.*/
    fun assertDryRun() {
        // TODO: テスト全体でスレッドが破棄されないためチェックできない
        // assertThat(getThreadByName()).isNull()
        Truth.assertThat(server.requestCount).isEqualTo(0)
    }
}
