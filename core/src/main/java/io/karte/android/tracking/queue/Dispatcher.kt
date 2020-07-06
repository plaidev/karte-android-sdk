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
package io.karte.android.tracking.queue

import android.os.Handler
import android.os.HandlerThread
import io.karte.android.KarteApp
import io.karte.android.core.library.ActionModule
import io.karte.android.core.library.TrackModule
import io.karte.android.core.logger.Logger
import io.karte.android.tracking.TrackCompletion
import io.karte.android.tracking.client.TrackResponse
import io.karte.android.tracking.client.requestOf
import io.karte.android.utilities.connectivity.Connectivity
import io.karte.android.utilities.connectivity.retryIntervalMs
import io.karte.android.utilities.datastore.DataStore
import io.karte.android.utilities.datastore.RelationalOperator
import io.karte.android.utilities.http.Client
import kotlin.math.min

private const val LOG_TAG = "Karte.Dispatcher"
private const val MAX_RETRY_COUNT = 6
private const val DEFAULT_DELAY_MS = 500L

private data class GroupingKey(
    val visitorId: String,
    val originPvId: String,
    val pvId: String,
    val isRetry: Boolean
)

internal const val THREAD_NAME = "io.karte.android.Tracker"

internal class Dispatcher {
    private val thread = HandlerThread(THREAD_NAME, Thread.MIN_PRIORITY).apply { start() }
    private val handler: Handler = Handler(thread.looper)
    private val completions = mutableMapOf<Long, TrackCompletion>()
    private var isSuspend: Boolean = false
        set(value) {
            if (value) handler.removeCallbacks(::run)
            else handler.postDelayed(::run, DEFAULT_DELAY_MS)
            field = value
        }

    init {
        DataStore.setup(KarteApp.self.application.applicationContext, EventRecord.EventContract)
        KarteApp.self.connectivityObserver?.subscribe(::connectivity)
    }

    private fun connectivity(available: Boolean) {
        Logger.d(LOG_TAG, "connectivity changed: $available")
        isSuspend = !available
    }

    fun push(record: EventRecord, completion: TrackCompletion?) {
        Logger.d(LOG_TAG, "push event. ${record.event.eventName.value}")
        handler.post {
            val id = DataStore.put(record)
            completion?.let {
                if (id == -1L) {
                    Logger.e(LOG_TAG, "Failed to push Event to queue")
                    completion.onComplete(false)
                } else {
                    completions[id] = it
                }
            }
        }
        handler.postDelayed(::run, DEFAULT_DELAY_MS)
    }

    fun teardown() {
        DataStore.teardown()
        KarteApp.self.connectivityObserver?.unsubscribe(::connectivity)
    }

    private fun run() {
        val online = Connectivity.isOnline(KarteApp.self.application)
        Logger.d(LOG_TAG, "connectivity: $online.")
        if (!online) {
            Logger.v(LOG_TAG, "now connectivity is offline. suspend.")
            return
        }

        val records: MutableList<EventRecord> = mutableListOf()
        DataStore.transaction().use { tx ->
            records.addAll(
                tx.read(
                    EventRecord.EventContract,
                    listOf(
                        Triple(
                            EventRecord.EventContract.STATE,
                            RelationalOperator.Unequal,
                            EventRecord.State.Requesting.ordinal.toString()
                        )
                    )
                )
                    .map {
                        it.also { tx.update(it.apply { state = EventRecord.State.Requesting }) }
                    }
            )
            tx.success()
        }
        records.groupBy(
            { GroupingKey(it.visitorId, it.originalPvId, it.pvId, it.retry > 0) },
            { it })
            .forEach { events ->
                Logger.d(LOG_TAG, "request events: ${events.value.size}")
                val (visitorId, originalPvId, pvId) = events.key
                var request =
                    requestOf(
                        visitorId,
                        originalPvId,
                        pvId,
                        events.value.map { it.event.apply { isRetry = it.retry > 0 } })
                KarteApp.self.modules
                    .filterIsInstance<TrackModule>()
                    .forEach { request = it.intercept(request) }
                try {
                    val response = Client.execute(request)
                    Logger.d(LOG_TAG, "response: ${response.code}")
                    if (response.isSuccessful) {
                        if (!events.key.isRetry) {
                            KarteApp.self.modules.filterIsInstance<ActionModule>()
                                .forEach { it.receive(TrackResponse(response), request) }
                        }
                        events.value.forEach {
                            DataStore.delete(it)
                            completions.remove(it.id)?.onComplete(true)
                        }
                    } else {
                        Logger.e(LOG_TAG, "Failed to request. ${response.code}: '${response.body}'")
                        queueRetry(events.value)
                    }
                } catch (e: Exception) {
                    Logger.e(LOG_TAG, "Failed to send request.", e)
                    queueRetry(events.value)
                }
            }
    }

    private fun queueRetry(events: List<EventRecord>) {
        var minRetryCount = MAX_RETRY_COUNT
        events.forEach {
            val nextRetryCount = it.retry + 1
            if (nextRetryCount <= MAX_RETRY_COUNT && it.event.isRetryable) {
                DataStore.update(it.apply {
                    state = EventRecord.State.Failed
                    retry = nextRetryCount
                })
                minRetryCount = min(it.retry, minRetryCount)
            } else {
                val logMessage =
                    if (it.event.isRetryable) "The maximum number of retries has been reached."
                    else "This event is not retryable."
                Logger.w(LOG_TAG, logMessage)
                DataStore.delete(it)
            }
            completions.remove(it.id)?.onComplete(false)
        }
        if (minRetryCount > MAX_RETRY_COUNT) return
        val retryInterval = retryIntervalMs(minRetryCount)
        Logger.d(LOG_TAG, "Retry after $retryInterval ms. count $minRetryCount")
        handler.postDelayed(::run, retryInterval)
    }
}
