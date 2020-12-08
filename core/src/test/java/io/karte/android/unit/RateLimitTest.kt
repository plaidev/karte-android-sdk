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

import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import com.google.common.truth.Truth.assertThat
import io.karte.android.RobolectricTestCase
import io.karte.android.proceedBufferedCall
import io.karte.android.tracking.queue.RateLimit
import io.karte.android.tracking.queue.getCurrentTimeMillis
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.Before
import org.junit.Test

class RateLimitTest : RobolectricTestCase() {
    private val thread =
        HandlerThread("test_thread", Process.THREAD_PRIORITY_LOWEST).apply { start() }
    private val handler: Handler = Handler(thread.looper)
    private lateinit var rateLimit: RateLimit

    @Before
    fun setup() {
        rateLimit = RateLimit(handler, 5, 1000)
    }

    @Test
    fun 正常なリクエスト数なら許可されること() {
        assertThat(rateLimit.canRequest).isTrue()
        rateLimit.increment()
        assertThat(rateLimit.canRequest).isTrue()
        rateLimit.increment(3)
        assertThat(rateLimit.canRequest).isTrue()
    }

    @Test
    fun 過剰なリクエスト数なら制限されること() {
        assertThat(rateLimit.canRequest).isTrue()
        rateLimit.increment(10)
        assertThat(rateLimit.canRequest).isFalse()
    }

    @Test
    fun リクエスト数の上下で制限_許可が行われること() {
        assertThat(rateLimit.canRequest).isTrue()
        rateLimit.increment(5)
        assertThat(rateLimit.canRequest).isTrue()
        rateLimit.increment(5)
        assertThat(rateLimit.canRequest).isFalse()

        rateLimit.decrementWithDelay(3)
        proceedBufferedCall(thread)
        assertThat(rateLimit.canRequest).isFalse()
        rateLimit.decrementWithDelay(3)
        proceedBufferedCall(thread)
        assertThat(rateLimit.canRequest).isTrue()
    }

    @Test
    fun decrementが呼ばれなくても一定期間でlimitが解除されること() {
        mockkStatic("io.karte.android.tracking.queue.RateLimitKt")
        every { getCurrentTimeMillis() } returns 0
        assertThat(rateLimit.canRequest).isTrue()
        rateLimit.increment(10)
        assertThat(rateLimit.canRequest).isFalse()

        every { getCurrentTimeMillis() } returns 5000
        assertThat(rateLimit.canRequest).isTrue()
        unmockkStatic("io.karte.android.tracking.queue.RateLimitKt")
    }
}
