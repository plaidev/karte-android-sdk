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
package io.karte.android.unit

import com.google.common.truth.Truth.assertThat
import io.karte.android.utilities.connectivity.retryIntervalMs
import kotlin.random.Random
import org.junit.Test

class ExponentialBackoffTest {

    @Test
    fun returnsExpectedIntervalsForSeed42FromCount1To10() {
        val expected = listOf(
            363L, // count=1
            1452L, // count=2
            5810L, // count=3
            23242L, // count=4
            92968L, // count=5
            371873L, // count=6
            1487493L, // count=7
            5949974L, // count=8
            23799898L, // count=9
            95199594L // count=10
        )
        expected.forEachIndexed { index, expectedMs ->
            assertThat(retryIntervalMs(index + 1, random = Random(42L))).isEqualTo(expectedMs)
        }
    }

    @Test
    fun returnsExpectedIntervalsForSeed0FromCount1To10() {
        val expected = listOf(
            524L, // count=1
            2099L, // count=2
            8397L, // count=3
            33588L, // count=4
            134353L, // count=5
            537412L, // count=6
            2149648L, // count=7
            8598594L, // count=8
            34394379L, // count=9
            137577516L // count=10
        )
        expected.forEachIndexed { index, expectedMs ->
            assertThat(retryIntervalMs(index + 1, random = Random(0L))).isEqualTo(expectedMs)
        }
    }
}
