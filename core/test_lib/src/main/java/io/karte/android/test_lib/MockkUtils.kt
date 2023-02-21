//
//  Copyright 2023 PLAID, Inc.
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
package io.karte.android.test_lib

import android.util.Log
import io.mockk.Call
import io.mockk.MockKAnswerScope
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic

fun pipeLog() {
    mockkStatic(Log::class)
    val tagSlot = slot<String>()
    val msgSlot = slot<String>()
    val ans: MockKAnswerScope<Int, Int>.(Call) -> Int =
        { kotlin.io.println("piped: ${tagSlot.captured} ${msgSlot.captured}"); 0 }
    every { Log.v(capture(tagSlot), capture(msgSlot)) } answers (ans)
    every { Log.v(capture(tagSlot), capture(msgSlot), any()) } answers (ans)
    every { Log.d(capture(tagSlot), capture(msgSlot)) } answers (ans)
    every { Log.d(capture(tagSlot), capture(msgSlot), any()) } answers (ans)
    every { Log.i(capture(tagSlot), capture(msgSlot)) } answers (ans)
    every { Log.i(capture(tagSlot), capture(msgSlot), any()) } answers (ans)
    every { Log.w(capture(tagSlot), capture(msgSlot)) } answers (ans)
    every { Log.w(capture(tagSlot), capture(msgSlot), any()) } answers (ans)
    every { Log.e(capture(tagSlot), capture(msgSlot)) } answers (ans)
    every { Log.e(capture(tagSlot), capture(msgSlot), any()) } answers (ans)
}

fun unpipeLog() {
    unmockkStatic(Log::class)
}
