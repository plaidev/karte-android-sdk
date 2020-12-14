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
package io.karte.android.inappmessaging.unit

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24])
class MessageModelTest {

    @Test
    fun モデルの初期化_popupモデル() {
        val model = createMessagePopup()
        Assert.assertEquals(true, model.shouldLoad())
    }

    @Test
    fun モデルの初期化_remote_configモデル() {
        val model = createMessageRemoteConfig()
        Assert.assertEquals(false, model.shouldLoad())
    }

    @Test
    fun モデルの初期化_control_groupモデル() {
        val model = createMessageControlGroup()
        Assert.assertEquals(true, model.shouldLoad())
    }
}
