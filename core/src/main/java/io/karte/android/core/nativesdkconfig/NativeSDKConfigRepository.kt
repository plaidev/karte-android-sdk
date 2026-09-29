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
package io.karte.android.core.nativesdkconfig

import io.karte.android.core.logger.Logger
import io.karte.android.core.repository.Repository
import kotlin.time.Duration.Companion.seconds
import org.json.JSONObject

private const val LOG_TAG = "NativeSDKConfigRepository"
private const val KEY_CACHE = "native_sdk_config"

internal class NativeSDKConfigRepository(private val repository: Repository) {

    fun load(): NativeSDKConfigCache? {
        val (flagsObject, fetchedAt, ttl) = try {
            val jsonString = repository.get<String?>(KEY_CACHE, null) ?: return null
            val jsonObject = JSONObject(jsonString)
            Triple(
                jsonObject.getJSONObject("flags"),
                jsonObject.getLong("fetchedAt"),
                jsonObject.getLong("ttl").seconds
            )
        } catch (e: Exception) {
            Logger.e(LOG_TAG, "NativeSDKConfig cache load failed", e)
            repository.remove(KEY_CACHE)
            return null
        }
        val flags = flagsObject.keys().asSequence()
            .mapNotNull { key ->
                (flagsObject.opt(key) as? Boolean)?.let { boolValue -> key to boolValue }
            }
            .toMap()
        return NativeSDKConfigCache(flags, fetchedAt, ttl)
    }

    fun save(cache: NativeSDKConfigCache) {
        val flagsObject = JSONObject()
        val jsonString = try {
            cache.flags.forEach { (key, value) -> flagsObject.put(key, value) }
            JSONObject()
                .put("flags", flagsObject)
                .put("fetchedAt", cache.fetchedAt)
                .put("ttl", cache.ttl.inWholeSeconds)
                .toString()
        } catch (e: Exception) {
            Logger.e(LOG_TAG, "NativeSDKConfig cache encode failed", e)
            return
        }
        repository.put(KEY_CACHE, jsonString)
    }
}
