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

import io.karte.android.shadow.CustomShadowWebView
import io.karte.android.utilities.isAscii
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Suppress("NonAsciiCharacters")
@RunWith(RobolectricTestRunner::class)
@Config(packageName = "io.karte.android.core", sdk = [24], shadows = [CustomShadowWebView::class])
class UtilsTest {

    @Test
    fun countainsMultibyte() {
        Assert.assertFalse("あ".isAscii())
        Assert.assertFalse("ア".isAscii())
        Assert.assertFalse("ｲ".isAscii())
        Assert.assertFalse("　".isAscii())
        Assert.assertFalse("testテスト".isAscii())
        Assert.assertFalse("⛔".isAscii())
        Assert.assertFalse("👪".isAscii())
        Assert.assertFalse(null?.isAscii() ?: false)
        Assert.assertFalse("".isAscii())

        Assert.assertTrue("view".isAscii())
        Assert.assertTrue("test-event".isAscii())
        Assert.assertTrue("0123456789".isAscii())
        Assert.assertTrue(" ".isAscii())
        Assert.assertTrue("_".isAscii())
    }
}
