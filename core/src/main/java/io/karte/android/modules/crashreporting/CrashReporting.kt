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
package io.karte.android.modules.crashreporting

import io.karte.android.BuildConfig
import io.karte.android.KarteApp
import io.karte.android.core.library.Library
import io.karte.android.tracking.Event
import io.karte.android.tracking.EventName
import io.karte.android.tracking.Tracker
import io.karte.android.tracking.Values
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Date

internal class CrashReporting : Thread.UncaughtExceptionHandler, Library {

    private val handler: Thread.UncaughtExceptionHandler? =
        Thread.getDefaultUncaughtExceptionHandler()

    //region Library
    override val name: String = "CrashReporting"
    override val version: String = BuildConfig.VERSION_NAME
    override val isPublic: Boolean = false

    override fun configure(app: KarteApp) {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun unconfigure(app: KarteApp) {
        Thread.setDefaultUncaughtExceptionHandler(handler)
    }
    //endregion

    //region UncaughtExceptionHandler
    override fun uncaughtException(thread: Thread, th: Throwable) {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        th.printStackTrace(pw)
        pw.flush()

        Tracker.track(
            CrashEvent(
                mapOf(
                    "error_info" to mapOf<String, Any?>(
                        "message" to th.localizedMessage,
                        "reason" to th.cause.toString(),
                        "stack_trace" to sw.toString().take(30000),
                        "crash_date" to Date().time / 1000
                    )
                )
            )
        )
        if (this.handler != null) {
            this.handler.uncaughtException(thread, th)
        } else {
            try {
                Thread.sleep(500)
            } catch (e1: InterruptedException) {
            }

            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(10)
        }
    }
    //endregion
}

private class CrashEvent(values: Values) : Event(CrashEventName(), values)

private class CrashEventName(override val value: String = "native_app_crashed") : EventName
