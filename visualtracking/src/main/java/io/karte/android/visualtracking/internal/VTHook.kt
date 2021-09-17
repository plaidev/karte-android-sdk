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
package io.karte.android.visualtracking.internal

import io.karte.android.core.logger.Logger
import io.karte.android.visualtracking.VisualTracking

internal object VTHook {
    private const val LOG_TAG = "Karte.VTHook"

    @JvmStatic
    fun hookAction(name: String, args: Array<Any>) {
        try {
            Logger.d(LOG_TAG, "hookAction name=$name, " +
                "args=${args.joinToString(transform = Any::toString)}")
            val instance = VisualTracking.self
            if (instance == null) {
                Logger.e(LOG_TAG, "Tried to hook action but VisualTracking is not enabled.")
                return
            }
            instance.handleAction(name, args)
        } catch (e: Exception) {
            Logger.e(LOG_TAG, "Failed to handle action.", e)
        }
    }

    @JvmStatic
    fun hookDynamicInvoke(args: Array<Any>) {
        Logger.d(LOG_TAG, "hookDynamicInvoke")
        try {
            val throwable = Throwable()
            val method = throwable.stackTrace
                .asSequence()
                .mapNotNull { HookTargetMethodFromDynamicInvoke.from(it) }
                .firstOrNull()

            if (method == null) {
                Logger.d(LOG_TAG, "Hook target no found in stack trace.")
                return
            }

            hookAction(method.actionName, args)
        } catch (e: Exception) {
            Logger.e(LOG_TAG, "Failed to handle action.", e)
        }
    }
}
