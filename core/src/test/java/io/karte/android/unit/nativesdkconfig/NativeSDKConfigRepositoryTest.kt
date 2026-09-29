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

import io.karte.android.core.nativesdkconfig.NativeSDKConfigCache
import io.karte.android.core.nativesdkconfig.NativeSDKConfigRepository
import io.karte.android.core.repository.PreferenceRepository
import io.karte.android.test_lib.RobolectricTestCase
import io.karte.android.test_lib.application
import kotlin.time.Duration.Companion.seconds
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

private const val APP_KEY = "testappkey_1234567890123456789"
private const val KEY_CACHE = "native_sdk_config"

private class RepositoryFixture(namespace: String) : AutoCloseable {
    val prefsRepository: PreferenceRepository
    val repository: NativeSDKConfigRepository

    init {
        prefsRepository = PreferenceRepository(application(), APP_KEY, namespace)
        repository = NativeSDKConfigRepository(prefsRepository)
    }

    override fun close() {
        prefsRepository.removeAll()
    }
}

class NativeSDKConfigRepositoryTest : RobolectricTestCase() {

    @Test
    fun testWhenNoCache() {
        RepositoryFixture("NativeSDKConfigRepositoryTest.testWhenNoCache").use { fixture ->
            assertNull(fixture.repository.load())
        }
    }

    @Test
    fun testWhenValidCache() {
        RepositoryFixture("NativeSDKConfigRepositoryTest.testWhenValidCache").use { fixture ->
            val json = """{"flags":{"flag_a":true,"flag_b":false},"fetchedAt":123456789,"ttl":3600}"""
            fixture.prefsRepository.put(KEY_CACHE, json)

            val loaded = fixture.repository.load()
            assertNotNull(loaded)
            assertEquals(true, loaded!!.flags["flag_a"])
            assertEquals(false, loaded.flags["flag_b"])
            assertEquals(123456789L, loaded.fetchedAt)
        }
    }

    @Test
    fun testWhenValidCacheWithEmptyFlags() {
        RepositoryFixture("NativeSDKConfigRepositoryTest.testWhenValidCacheWithEmptyFlags").use { fixture ->
            fixture.prefsRepository.put(KEY_CACHE, """{"flags":{},"fetchedAt":123456789,"ttl":3600}""")

            val loaded = fixture.repository.load()
            assertNotNull(loaded)
            assertEquals(emptyMap<String, Boolean>(), loaded!!.flags)
            assertEquals(123456789L, loaded.fetchedAt)
        }
    }

    @Test
    fun testWhenInvalidJson() {
        RepositoryFixture("NativeSDKConfigRepositoryTest.testWhenInvalidJson").use { fixture ->
            fixture.prefsRepository.put(KEY_CACHE, "not valid json{")

            assertNull(fixture.repository.load())
            assertNull(fixture.repository.load())
        }
    }

    @Test
    fun testWhenCacheHasUnexpectedType() {
        RepositoryFixture("NativeSDKConfigRepositoryTest.testWhenCacheHasUnexpectedType").use { fixture ->
            fixture.prefsRepository.put(KEY_CACHE, 123)

            assertNull(fixture.repository.load())
            assertNull(fixture.repository.load())
        }
    }

    @Test
    fun testWhenInvalidCacheWithMissingFlags() {
        RepositoryFixture("NativeSDKConfigRepositoryTest.testWhenInvalidCacheWithMissingFlags").use { fixture ->
            fixture.prefsRepository.put(KEY_CACHE, """{"fetchedAt":100}""")

            assertNull(fixture.repository.load())
            assertNull(fixture.repository.load())
        }
    }

    @Test
    fun testWhenInvalidCacheWithMissingFetchedAt() {
        RepositoryFixture("NativeSDKConfigRepositoryTest.testWhenInvalidCacheWithMissingFetchedAt").use { fixture ->
            fixture.prefsRepository.put(KEY_CACHE, """{"flags":{"flag_a":true}}""")

            assertNull(fixture.repository.load())
            assertNull(fixture.repository.load())
        }
    }

    @Test
    fun testIgnoresInvalidFlagValue() {
        RepositoryFixture("NativeSDKConfigRepositoryTest.testIgnoresInvalidFlagValue").use { fixture ->
            val json = """{"flags":{"bool_flag":true,"str_flag":"value"},"fetchedAt":100,"ttl":3600}"""
            fixture.prefsRepository.put(KEY_CACHE, json)

            val loaded = fixture.repository.load()
            assertNotNull(loaded)
            assertEquals(true, loaded!!.flags["bool_flag"])
            assertNull(loaded.flags["str_flag"])
        }
    }

    @Test
    fun testSave() {
        RepositoryFixture("NativeSDKConfigRepositoryTest.testSave").use { fixture ->
            val flags = mapOf("flag_a" to true, "flag_b" to false)
            val fetchedAt = 123456789L
            fixture.repository.save(NativeSDKConfigCache(flags, fetchedAt, 3600.seconds))

            val stored = fixture.prefsRepository.get<String?>(KEY_CACHE, null)
            assertNotNull(stored)
            val json = JSONObject(stored!!)
            assertEquals(fetchedAt, json.getLong("fetchedAt"))
            assertEquals(3600, json.getLong("ttl"))
            val flagsObject = json.getJSONObject("flags")
            assertEquals(true, flagsObject.getBoolean("flag_a"))
            assertEquals(false, flagsObject.getBoolean("flag_b"))
        }
    }
}
