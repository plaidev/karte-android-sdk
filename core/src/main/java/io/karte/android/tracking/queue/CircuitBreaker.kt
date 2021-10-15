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

private const val TIME_RECOVER_AFTER_MS = 300000L

internal fun getCurrentTimeMillisCB(): Long = System.currentTimeMillis()

internal class CircuitBreaker(
    private val threshold: Int = 3,
    private val recoverAfter: Long = TIME_RECOVER_AFTER_MS
) {
    private var failureCount = 0
    private var lastFailedAt = -1L

    val canRequest: Boolean
        get() {
            if ((getCurrentTimeMillisCB() - lastFailedAt) > recoverAfter) reset()
            return failureCount < threshold
        }

    fun recordFailure() {
        failureCount ++
        lastFailedAt = getCurrentTimeMillisCB()
    }

    fun reset() {
        failureCount = 0
        lastFailedAt = -1
    }
}
