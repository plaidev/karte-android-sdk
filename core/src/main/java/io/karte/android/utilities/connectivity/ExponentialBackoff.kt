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
package io.karte.android.utilities.connectivity

import kotlin.math.pow
import kotlin.random.Random

internal fun retryIntervalMs(
    count: Int,
    retryIntervalSec: Double = 0.5,
    multiplier: Double = 4.0,
    randomFactor: Double = 0.5
): Long {
    val interval = retryIntervalSec * multiplier.pow(count - 1)
    val randomMs = Random.nextDouble(1 - randomFactor, 1 + randomFactor)
    return (interval * randomMs * 1000).toLong()
}
