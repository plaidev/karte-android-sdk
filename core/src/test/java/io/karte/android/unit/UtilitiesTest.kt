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
import io.karte.android.tracking.valuesOf
import io.karte.android.utilities.isAscii
import io.karte.android.utilities.merge
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Suppress("NonAsciiCharacters")
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24], shadows = [CustomShadowWebView::class])
class UtilitiesTest {

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

    @Test
    fun merge() {
        val baseString = """
            {
                "f1": {
                    "f1a": {
                        "f1a1": "f1a1v",
                        "f1a3": "f1a3v"
                    }
                },
                "f2": "f2v",
                "f4": {
                    "f4a": "f4av"
                }
            }
        """.trimIndent()
        val additionalString = """
            {
                "f1": {
                    "f1a": {
                        "f1a2": "f1a2v",
                        "f1a3": "f1a3v2"
                    }
                },
                "f3": "f3v",
                "f4": "f4v"
            }
        """.trimIndent()
        val base = valuesOf(baseString)
        val additional = valuesOf(additionalString)
        val merged = base.merge(additional)

        Assert.assertEquals(value(merged, "f1.f1a.f1a1"), "f1a1v")
        Assert.assertEquals(value(merged, "f1.f1a.f1a2"), "f1a2v")
        Assert.assertEquals(value(merged, "f1.f1a.f1a3"), "f1a3v2")
        Assert.assertEquals(value(merged, "f2"), "f2v")
        Assert.assertEquals(value(merged, "f3"), "f3v")
        Assert.assertEquals(value(merged, "f4"), "f4v")

        Assert.assertNull(value(base, "f1.f1a.f1a2"))
        Assert.assertNull(value(base, "f3"))
    }

    private fun value(values: Map<String, Any>, path: String): Any? {
        val components = path.split(".", ignoreCase = false, limit = 2)
        return when (components.size) {
            0 -> null
            1 -> values[components[0]]
            else -> {
                val v = values[components[0]]
                if (v is Map<*, *>) {
                    @Suppress("UNCHECKED_CAST")
                    value(v as Map<String, Any>, components[1])
                } else {
                    null
                }
            }
        }
    }
}
