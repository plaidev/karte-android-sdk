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

private const val LIMIT_PER_WINDOW = 200
private const val TIME_WINDOW_MS = 60000L

internal fun getCurrentTimeMillis(): Long = System.currentTimeMillis()

internal class RateLimit(
    private val handler: Handler,
    private val limit: Int = LIMIT_PER_WINDOW,
    private val window: Long = TIME_WINDOW_MS
) {
    private var count = 0
    private var lastIncrementedAt = getCurrentTimeMillis()

    val canRequest: Boolean
        get() {
            // decrement漏れ対応
            if (lastIncrementedAt < getCurrentTimeMillis() - window * 2) {
                count = 0
            }
            return count <= limit
        }

    fun increment(delta: Int = 1) {
        count += delta
        lastIncrementedAt = getCurrentTimeMillis()
    }

    fun decrementWithDelay(delta: Int = 1, ops: (() -> Unit)? = null) {
        handler.postDelayed({
            count -= delta
            if (count < 0) count = 0
            ops?.invoke()
        }, window)
    }
}
