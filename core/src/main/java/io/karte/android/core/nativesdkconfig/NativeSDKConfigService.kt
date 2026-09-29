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

import android.os.Handler
import android.os.Looper
import android.os.Process
import io.karte.android.core.logger.Logger
import io.karte.android.core.repository.Repository
import io.karte.android.tracking.Event
import io.karte.android.tracking.EventName
import io.karte.android.tracking.Tracker
import io.karte.android.utilities.connectivity.retryIntervalMs
import io.karte.android.utilities.http.Client
import io.karte.android.utilities.http.Response
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.concurrent.Volatile
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import org.json.JSONException
import org.json.JSONObject

private const val LOG_TAG = "NativeSDKConfigService"

internal class NativeSDKConfigService(appKey: String, cdnBaseUrl: String, repository: Repository) {
    private val repository = NativeSDKConfigRepository(repository)
    private val fetcher = NativeSDKConfigFetcher(appKey, cdnBaseUrl)
    private val mainHandler = Handler(Looper.getMainLooper())

    // NOTE: flagsは高頻度で参照されるためメモリ上にキャッシュしている (fetchedAtは参照頻度低いためディスクのみに保持)。
    // NOTE: 書き込みは直列で行われるため、スレッドセーフにする必要はない。
    // バックグラウンドスレッドでの更新後に、他スレッドがキャッシュから読み取るのを防止するため、Volatileとする。
    @Volatile private var flags: Map<String, Boolean> = emptyMap()

    // NOTE: corePoolSize=0とすることで、バックグラウンドスレッドが常駐してリソースが消費されるのを防ぐ。
    private val fetchExecutor = ThreadPoolExecutor(
        0,
        1,
        60L, // 60秒間idleだったら、スレッドを終了させる
        TimeUnit.SECONDS,
        SynchronousQueue(), // 実行中の場合は破棄する。フェッチを高頻度で行う必要はない。
        { runnable ->
            Thread({
                // Androidのスレッド優先度は実行中のスレッドに設定する。
                // https://developer.android.com/reference/android/os/Process#setThreadPriority(int)
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                runnable.run()
            }, "io.karte.android.NativeSDKConfig")
        },
        { _, executor ->
            if (executor.isShutdown) {
                Logger.d(LOG_TAG, "fetch discarded: executor is shutting down")
            } else {
                Logger.d(LOG_TAG, "fetch discarded: fetch already in progress")
            }
        }
    )

    init {
        this.repository.load()?.let { stored ->
            flags = stored.flags
        }
    }

    internal fun isEnabled(name: String, default: Boolean): Boolean = flags[name] ?: default

    // フォアグラウンド遷移のたびにキャッシュの有効期限を確認し、TTL超過時のみバックグラウンドでフェッチする
    internal fun fetchIfNeeded() {
        val cache = repository.load()
        if (cache != null && !cache.isExpired()) return
        fetchExecutor.execute { fetch() }
    }

    private fun fetch() {
        val response = fetcher.fetch() ?: return
        val json = try {
            JSONObject(response.body)
        } catch (e: JSONException) {
            Logger.e(LOG_TAG, "NativeSDKConfig fetch failed", e)
            return
        }
        val newFlags = parseFlags(json)
        val cache =
            NativeSDKConfigCache(newFlags, fetchedAt = System.currentTimeMillis(), ttl = response.cacheControlMaxAge())
        repository.save(cache)
        flags = newFlags
        // NOTE: Tracker.trackは通常通りメインスレッドで呼ぶ。
        mainHandler.post { Tracker.track(FetchNativeSDKConfigEvent(newFlags)) }
    }

    private fun parseFlags(jsonObject: JSONObject): Map<String, Boolean> = jsonObject.keys().asSequence()
        .mapNotNull { key ->
            (jsonObject.opt(key) as? Boolean)?.let { boolValue -> key to boolValue }
        }
        .toMap()

    internal fun teardown() {
        fetchExecutor.shutdown()
    }
}

private class NativeSDKConfigFetcher(private val appKey: String, private val cdnBaseUrl: String) {
    fun fetch(): Response? {
        val req = NativeSDKConfigRequest(cdnBaseUrl, appKey)
        for (attempt in 1..MAX_ATTEMPTS) {
            if (Thread.currentThread().isInterrupted) return null
            val response = try {
                Client.execute(req)
            } catch (e: Exception) {
                if (attempt == MAX_ATTEMPTS) {
                    Logger.d(LOG_TAG, "NativeSDKConfig fetch failed after $MAX_ATTEMPTS attempts: $e", e)
                    break
                }
                Logger.d(LOG_TAG, "NativeSDKConfig fetch failed (attempt $attempt): $e. Retrying...")
                sleepBeforeRetry(attempt)
                continue
            }
            if (response.isSuccessful) return response
            // NOTE: 5xx以外の場合には、リクエストに問題があったとみなし、リトライを打ち切る。
            if (response.code !in 500..599) {
                Logger.d(LOG_TAG, "NativeSDKConfig fetch failed: HTTP ${response.code}")
                return null
            }
            if (attempt == MAX_ATTEMPTS) {
                Logger.d(LOG_TAG, "NativeSDKConfig fetch failed after $MAX_ATTEMPTS attempts: HTTP ${response.code}")
                break
            }
            Logger.d(LOG_TAG, "NativeSDKConfig fetch failed (attempt $attempt): HTTP ${response.code}. Retrying...")
            sleepBeforeRetry(attempt)
        }
        return null
    }

    // NOTE: 初回 + リトライ (2回まで) で、最大で3回リクエストを試行する。
    // 1回目のインターバルは250〜750msの間、2回目のインターバルは1〜3秒の間である。
    // 専用のバックグラウンドスレッドを使用している、かつフェッチの頻度が低いため、設計の簡略化のためにThread.sleepを使用している。
    private fun sleepBeforeRetry(attempt: Int) {
        try {
            Thread.sleep(retryIntervalMs(attempt))
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
        const val LOG_TAG = "NativeSDKConfigFetcher"
    }
}

private class FetchNativeSDKConfigEvent(flags: Map<String, Boolean>) :
    Event(
        NativeSDKConfigEventName.FetchNativeSDKConfig,
        values = mapOf("flags" to flags)
    )

private enum class NativeSDKConfigEventName(override val value: String) : EventName {
    FetchNativeSDKConfig("_fetch_native_sdk_config")
}

internal fun Response.cacheControlMaxAge(): Duration {
    val cacheControlValues = headers.entries
        .firstOrNull { it.key.equals("Cache-Control", ignoreCase = true) }
        ?.value
        ?: return NativeSDKConfigCacheControl.FALLBACK_TTL

    val maxAgePrefix = "max-age="
    for (cacheControlValue in cacheControlValues) {
        for (directive in cacheControlValue.split(",")) {
            val trimmed = directive.trim().lowercase()
            if (!trimmed.startsWith(maxAgePrefix)) continue
            val value = trimmed.drop(maxAgePrefix.length)
            val seconds = value.toIntOrNull() ?: return NativeSDKConfigCacheControl.FALLBACK_TTL
            // NOTE: 想定外の値が入るのを防ぐために、60s〜3600sの範囲に切り詰める。
            return seconds.seconds.coerceIn(60.seconds, 3600.seconds)
        }
    }

    return NativeSDKConfigCacheControl.FALLBACK_TTL
}

private object NativeSDKConfigCacheControl {
    val FALLBACK_TTL = 300.seconds
}
