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

package io.karte.android.core.logger

import android.util.Log

internal interface Appender {
    fun append(log: LogEvent)
}

internal class ConsoleAppender : Appender {
    override fun append(log: LogEvent) {
        if (Logger.level > log.level) return
        when {
            log.level == LogLevel.VERBOSE && log.throwable == null ->
                Log.v(log.tag, log.message)
            log.level == LogLevel.VERBOSE && log.throwable != null ->
                Log.v(log.tag, log.message, log.throwable)
            log.level == LogLevel.DEBUG && log.throwable == null ->
                Log.d(log.tag, log.message)
            log.level == LogLevel.DEBUG && log.throwable != null ->
                Log.d(log.tag, log.message, log.throwable)
            log.level == LogLevel.INFO && log.throwable == null ->
                Log.i(log.tag, log.message)
            log.level == LogLevel.INFO && log.throwable != null ->
                Log.i(log.tag, log.message, log.throwable)
            log.level == LogLevel.WARN && log.throwable == null ->
                Log.w(log.tag, log.message)
            log.level == LogLevel.WARN && log.throwable != null ->
                Log.w(log.tag, log.message, log.throwable)
            log.level == LogLevel.ERROR && log.throwable == null ->
                Log.e(log.tag, log.message)
            log.level == LogLevel.ERROR && log.throwable != null ->
                Log.e(log.tag, log.message, log.throwable)
        }
    }
}
