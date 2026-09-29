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
package io.karte.android.unit.nativesdkconfig

import com.google.common.truth.Truth.assertThat
import io.karte.android.core.nativesdkconfig.NativeSDKConfigCache
import io.karte.android.core.nativesdkconfig.NativeSDKConfigRepository
import io.karte.android.core.nativesdkconfig.NativeSDKConfigService
import io.karte.android.core.nativesdkconfig.cacheControlMaxAge
import io.karte.android.core.repository.PreferenceRepository
import io.karte.android.test_lib.RobolectricTestCase
import io.karte.android.test_lib.application
import io.karte.android.test_lib.proceedUiBufferedCall
import io.karte.android.tracking.Event
import io.karte.android.tracking.Tracker
import io.karte.android.utilities.http.Client
import io.karte.android.utilities.http.Response
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

private const val CDN_BASE_URL = "https://cdn.example.com"
private const val APP_KEY = "testappkey_1234567890123456789"

private class ServiceFixture(namespace: String, existingCache: NativeSDKConfigCache?) : AutoCloseable {
    val prefsRepository: PreferenceRepository
    val service: NativeSDKConfigService

    init {
        mockkObject(Client)
        mockkStatic(Tracker::class)
        every { Tracker.track(any<Event>()) } just Runs
        prefsRepository = PreferenceRepository(application(), APP_KEY, namespace)
        existingCache?.let { NativeSDKConfigRepository(prefsRepository).save(it) }
        service = NativeSDKConfigService(APP_KEY, CDN_BASE_URL, prefsRepository)
    }

    override fun close() {
        service.teardown()
        prefsRepository.removeAll()
        unmockkObject(Client)
        unmockkStatic(Tracker::class)
    }
}

// `fetchIfNeeded`で起動したフェッチ用のバックグラウンドタスクが完了するまで待機する。
private fun waitForFetchToComplete(service: NativeSDKConfigService, completedBefore: Long) {
    val executor = service.fetchExecutorForTest()
    repeat(500) {
        if (executor.completedTaskCount > completedBefore) return
        Thread.sleep(10)
    }
    throw AssertionError("Background task did not finish by deadline")
}

@Suppress("UNCHECKED_CAST")
private fun NativeSDKConfigService.fetchExecutorForTest(): ThreadPoolExecutor {
    val field = NativeSDKConfigService::class.java.getDeclaredField("fetchExecutor")
    field.isAccessible = true
    return field.get(this) as ThreadPoolExecutor
}

private fun verifyFetchNativeSDKConfigEventSent(expectedFlags: Map<String, Boolean>) {
    val eventSlot = slot<Event>()
    verify(exactly = 1) { Tracker.track(capture(eventSlot)) }
    assertEquals("_fetch_native_sdk_config", eventSlot.captured.eventName.value)
    assertTrue(eventSlot.captured.isRetryable)
    val flags = eventSlot.captured.values.getJSONObject("flags")
    assertEquals(expectedFlags.size, flags.length())
    for ((key, value) in expectedFlags) {
        assertEquals(value, flags.getBoolean(key))
    }
}

class NativeSDKConfigServiceTest : RobolectricTestCase() {

    @Test
    fun testCacheControlMaxAge() {
        val cases = listOf(
            mapOf("Cache-Control" to listOf("public s-maxage=3600, max-age=1800")) to 1800.seconds,
            mapOf("Cache-Control" to listOf("max-age=1800")) to 1800.seconds,
            mapOf("Cache-Control" to listOf("max-age=1")) to 60.seconds, // 最小値の60に切り上げられる。
            mapOf("Cache-Control" to listOf("max-age=9999")) to 3600.seconds, // 最大値の3600に切り詰められる。
            emptyMap<String, List<String>>() to 300.seconds, // デフォルト値にフォールバック
            mapOf("Cache-Control" to listOf("  max-age=1800  ")) to 1800.seconds, // 前後の空白は許容される。
            mapOf("Cache-Control" to listOf("max-age = 1800")) to 300.seconds, // max-ageと=の間の空白は許容しない。デフォルト値にフォールバック
            mapOf("Cache-Control" to listOf("s-maxage=3600")) to 300.seconds, // デフォルト値にフォールバック
            mapOf("Cache-Control" to listOf("max-age=abc")) to 300.seconds, // デフォルト値にフォールバック
            mapOf("Cache-Control" to listOf("public", "max-age=1800")) to 1800.seconds
        )
        for ((headers, expectedTtl) in cases) {
            assertThat(Response(200, headers, "").cacheControlMaxAge()).isEqualTo(expectedTtl)
        }
    }

    @Test
    fun testInitWithExistingCache() {
        ServiceFixture(
            namespace = "NativeSDKConfigServiceTest.testInitWithExistingCache",
            existingCache = NativeSDKConfigCache(
                mapOf("feature_x" to true, "feature_y" to false),
                fetchedAt = 1_577_836_800_000L, // 2020-01-01T00:00:00Z (古いキャッシュでもロードされることのテストのために、十分古い日付を使用)
                ttl = 3600.seconds
            )
        ).use { fixture ->
            assertTrue(fixture.service.isEnabled("feature_x", default = false))
            assertFalse(fixture.service.isEnabled("feature_y", default = true))
            assertFalse(fixture.service.isEnabled("unknown_flag", default = false))
            assertTrue(fixture.service.isEnabled("unknown_flag", default = true))
        }
    }

    @Test
    fun testInitWithNoCache() {
        ServiceFixture(
            namespace = "NativeSDKConfigServiceTest.testInitWithNoCache",
            existingCache = null
        ).use { fixture ->
            assertFalse(fixture.service.isEnabled("feature_x", default = false))
            assertTrue(fixture.service.isEnabled("feature_y", default = true))
        }
    }

    @Test
    fun testFetchIfNeededFetchesWhenCacheExpired() {
        ServiceFixture(
            namespace = "NativeSDKConfigServiceTest.testFetchIfNeededFetchesWhenCacheExpired",
            existingCache = NativeSDKConfigCache(
                mapOf("old_flag" to true),
                fetchedAt = 0L,
                ttl = 3600.seconds
            )
        ).use { fixture ->
            every { Client.execute(any()) } returns Response(200, hashMapOf(), """{"new_flag":true}""")

            val completedBefore = fixture.service.fetchExecutorForTest().completedTaskCount
            fixture.service.fetchIfNeeded()
            waitForFetchToComplete(fixture.service, completedBefore)
            proceedUiBufferedCall()

            verify(exactly = 1) { Client.execute(any()) }
            assertTrue(fixture.service.isEnabled("new_flag", false))
            assertFalse(fixture.service.isEnabled("old_flag", false))
            verifyFetchNativeSDKConfigEventSent(mapOf("new_flag" to true))
        }
    }

    @Test
    fun testFetchIfNeededDoesNotFetchWhenCacheValid() {
        ServiceFixture(
            namespace = "NativeSDKConfigServiceTest.testFetchIfNeededDoesNotFetchWhenCacheValid",
            existingCache = NativeSDKConfigCache(
                mapOf("cached_flag" to true),
                fetchedAt = System.currentTimeMillis(),
                ttl = 3600.seconds
            )
        ).use { fixture ->
            fixture.service.fetchIfNeeded()

            verify(exactly = 0) { Client.execute(any()) }
            verify(exactly = 0) { Tracker.track(any<Event>()) }
        }
    }

    @Test
    fun testFetchIfNeededFetchesWhenNoCache() {
        ServiceFixture(
            namespace = "NativeSDKConfigServiceTest.testFetchIfNeededFetchesWhenNoCache",
            existingCache = null
        ).use { fixture ->
            every { Client.execute(any()) } returns Response(200, hashMapOf(), """{"fresh_flag":false}""")

            val completedBefore = fixture.service.fetchExecutorForTest().completedTaskCount
            fixture.service.fetchIfNeeded()
            waitForFetchToComplete(fixture.service, completedBefore)
            proceedUiBufferedCall()

            verify(exactly = 1) { Client.execute(any()) }
            assertFalse(fixture.service.isEnabled("fresh_flag", true))
            verifyFetchNativeSDKConfigEventSent(mapOf("fresh_flag" to false))
        }
    }

    @Test
    fun testExtractsOnlyBooleanFlagsFromResponse() {
        ServiceFixture(
            namespace = "NativeSDKConfigServiceTest.testExtractsOnlyBooleanFlagsFromResponse",
            existingCache = null
        ).use { fixture ->
            every { Client.execute(any()) } returns Response(
                200,
                hashMapOf(),
                """{"bool_flag":true,"str_flag":"value","num_flag":42}"""
            )

            val completedBefore = fixture.service.fetchExecutorForTest().completedTaskCount
            fixture.service.fetchIfNeeded()
            waitForFetchToComplete(fixture.service, completedBefore)
            proceedUiBufferedCall()

            assertTrue(fixture.service.isEnabled("bool_flag", false))
            assertFalse(fixture.service.isEnabled("str_flag", false))
            assertFalse(fixture.service.isEnabled("num_flag", false))
            verifyFetchNativeSDKConfigEventSent(mapOf("bool_flag" to true))
        }
    }

    @Test
    fun testKeepsExistingFlagsOnFetchFailure() {
        ServiceFixture(
            namespace = "NativeSDKConfigServiceTest.testKeepsExistingFlagsOnFetchFailure",
            existingCache = NativeSDKConfigCache(
                mapOf("fallback_flag" to true),
                fetchedAt = 0L,
                ttl = 3600.seconds
            )
        ).use { fixture ->
            every { Client.execute(any()) } throws IOException("network error")

            val completedBefore = fixture.service.fetchExecutorForTest().completedTaskCount
            fixture.service.fetchIfNeeded()
            waitForFetchToComplete(fixture.service, completedBefore)

            assertTrue(fixture.service.isEnabled("fallback_flag", false))
            verify(exactly = 3) { Client.execute(any()) } // 3回までリトライされる
            verify(exactly = 0) { Tracker.track(any<Event>()) }
        }
    }

    @Test
    fun testKeepsExistingFlagsOnHttp5xxError() {
        ServiceFixture(
            namespace = "NativeSDKConfigServiceTest.testKeepsExistingFlagsOnHttp5xxError",
            existingCache = NativeSDKConfigCache(
                mapOf("existing_flag" to true),
                fetchedAt = 0L,
                ttl = 3600.seconds
            )
        ).use { fixture ->
            every { Client.execute(any()) } returns Response(500, hashMapOf(), "Internal Server Error")

            val completedBefore = fixture.service.fetchExecutorForTest().completedTaskCount
            fixture.service.fetchIfNeeded()
            waitForFetchToComplete(fixture.service, completedBefore)

            assertTrue(fixture.service.isEnabled("existing_flag", false))
            verify(exactly = 3) { Client.execute(any()) } // 3回までリトライされる
            verify(exactly = 0) { Tracker.track(any<Event>()) }
        }
    }

    @Test
    fun testDoesNotRetryOnHttp4xxError() {
        ServiceFixture(
            namespace = "NativeSDKConfigServiceTest.testDoesNotRetryOnHttp4xxError",
            existingCache = NativeSDKConfigCache(
                mapOf("existing_flag" to true),
                fetchedAt = 0L,
                ttl = 3600.seconds
            )
        ).use { fixture ->
            every { Client.execute(any()) } returns Response(400, hashMapOf(), "Bad Request")

            val completedBefore = fixture.service.fetchExecutorForTest().completedTaskCount
            fixture.service.fetchIfNeeded()
            waitForFetchToComplete(fixture.service, completedBefore)

            assertTrue(fixture.service.isEnabled("existing_flag", false))
            verify(exactly = 1) { Client.execute(any()) } // リトライされない
            verify(exactly = 0) { Tracker.track(any<Event>()) }
        }
    }

    @Test
    fun testRetriesAfterNetworkErrorAndSucceeds() {
        ServiceFixture(
            namespace = "NativeSDKConfigServiceTest.testRetriesAfterNetworkErrorAndSucceeds",
            existingCache = null
        ).use { fixture ->
            every { Client.execute(any()) } throws IOException("network error") andThen
                Response(200, hashMapOf(), """{"retried_flag":true}""")

            val completedBefore = fixture.service.fetchExecutorForTest().completedTaskCount
            fixture.service.fetchIfNeeded()
            waitForFetchToComplete(fixture.service, completedBefore)
            proceedUiBufferedCall()

            verify(exactly = 2) { Client.execute(any()) } // 初回失敗後、1回目のリトライで成功
            assertTrue(fixture.service.isEnabled("retried_flag", false))
            verifyFetchNativeSDKConfigEventSent(mapOf("retried_flag" to true))
        }
    }

    @Test
    fun testRetriesAfterHttp5xxAndSucceeds() {
        ServiceFixture(
            namespace = "NativeSDKConfigServiceTest.testRetriesAfterHttp5xxAndSucceeds",
            existingCache = null
        ).use { fixture ->
            every { Client.execute(any()) } returnsMany listOf(
                Response(500, hashMapOf(), "Internal Server Error"),
                Response(200, hashMapOf(), """{"retried_flag":true}""")
            )

            val completedBefore = fixture.service.fetchExecutorForTest().completedTaskCount
            fixture.service.fetchIfNeeded()
            waitForFetchToComplete(fixture.service, completedBefore)
            proceedUiBufferedCall()

            verify(exactly = 2) { Client.execute(any()) } // 初回失敗後、1回目のリトライで成功
            assertTrue(fixture.service.isEnabled("retried_flag", false))
            verifyFetchNativeSDKConfigEventSent(mapOf("retried_flag" to true))
        }
    }

    @Test
    fun testFetchIfNeededDiscardsDuplicateCallWhileFetching() {
        ServiceFixture(
            namespace = "NativeSDKConfigServiceTest.testFetchIfNeededDiscardsDuplicateCallWhileFetching",
            existingCache = NativeSDKConfigCache(
                mapOf("old_flag" to true),
                fetchedAt = 0L,
                ttl = 3600.seconds
            )
        ).use { fixture ->
            val fetchStarted = CountDownLatch(1)
            val releaseFetch = CountDownLatch(1)
            every { Client.execute(any()) } answers {
                fetchStarted.countDown()
                releaseFetch.await(5, TimeUnit.SECONDS) // 2回目のfetchIfNeeded()の呼び出しが行われるまで待機。
                Response(200, hashMapOf(), """{"flag":true}""")
            }

            val completedBefore = fixture.service.fetchExecutorForTest().completedTaskCount
            fixture.service.fetchIfNeeded()
            assertTrue(fetchStarted.await(5, TimeUnit.SECONDS))
            fixture.service.fetchIfNeeded() // 1回目のfetchIfNeeded()が待機している間に、次のfetchIfNeededを呼ぶ。
            releaseFetch.countDown() // 1回目のfetchIfNeeded()を再開させる。
            waitForFetchToComplete(fixture.service, completedBefore)

            // フェッチが実行中の場合、2回目以降のフェッチ要求は破棄される。
            verify(exactly = 1) { Client.execute(any()) }
        }
    }
}
