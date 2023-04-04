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

import com.google.common.truth.Truth.assertThat
import io.karte.android.test_lib.RobolectricTestCase
import io.karte.android.tracking.queue.CircuitBreaker
import io.karte.android.tracking.queue.getCurrentTimeMillisCB
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.Before
import org.junit.Test

class CircuitBreakerTest : RobolectricTestCase() {
    private lateinit var circuitBreaker: CircuitBreaker

    @Before
    fun setup() {
        circuitBreaker = CircuitBreaker(3, 300000L)
    }

    @Test
    fun 失敗数が閾値を超えなければ許可されること() {
        assertThat(circuitBreaker.canRequest).isTrue()
        circuitBreaker.recordFailure()
        assertThat(circuitBreaker.canRequest).isTrue()
        circuitBreaker.recordFailure()
        assertThat(circuitBreaker.canRequest).isTrue()
    }

    @Test
    fun 失敗数が閾値を超えたら制限されること() {
        circuitBreaker.recordFailure()
        circuitBreaker.recordFailure()
        circuitBreaker.recordFailure()
        assertThat(circuitBreaker.canRequest).isFalse()
    }

    @Test
    fun 制限がリセットにより解除されること() {
        circuitBreaker.recordFailure()
        circuitBreaker.recordFailure()
        circuitBreaker.recordFailure()
        assertThat(circuitBreaker.canRequest).isFalse()

        circuitBreaker.reset()
        assertThat(circuitBreaker.canRequest).isTrue()

        circuitBreaker.recordFailure()
        circuitBreaker.recordFailure()
        circuitBreaker.recordFailure()
        assertThat(circuitBreaker.canRequest).isFalse()
    }

    @Test
    fun リセットが呼ばれなくても一定期間で制限が解除されること() {
        mockkStatic("io.karte.android.tracking.queue.CircuitBreakerKt")
        every { getCurrentTimeMillisCB() } returns 1
        circuitBreaker.recordFailure()
        circuitBreaker.recordFailure()
        circuitBreaker.recordFailure()
        assertThat(circuitBreaker.canRequest).isFalse()

        every { getCurrentTimeMillisCB() } returns 300002
        assertThat(circuitBreaker.canRequest).isTrue()
        unmockkStatic("io.karte.android.tracking.queue.CircuitBreakerKt")
    }
}
