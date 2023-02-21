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

import android.os.Looper
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowLooper

fun getThreadByName(threadName: String = InternalUtils.threadName): Thread? {
    // NOTE: idがincrementalに振られることを信頼している
    return Thread.getAllStackTraces().keys.filter {
        it.name == threadName
    }.maxBy { it.id }
}

private fun getLooperByThreadName(threadName: String): Looper? {
    val thread = getThreadByName(threadName)
    return ShadowLooper.getAllLoopers().firstOrNull {
        it.thread.id == thread?.id
    }
}

private fun getLooperByThread(thread: Thread): Looper? {
    return ShadowLooper.getAllLoopers().firstOrNull {
        it.thread == thread
    }
}

fun proceedBufferedCall(thread: Thread? = null, threadName: String? = null) {
    val name = thread?.name ?: threadName ?: InternalUtils.threadName
    val looper = if (thread != null) {
        getLooperByThread(thread)
    } else {
        getLooperByThreadName(name)
    }
    if (looper == null) {
        println("proceedBufferedCall: $name is not found")
        return
    }
    println("proceedBufferedCall: $name")
    Shadows.shadowOf(looper).runToEndOfTasks()
}
