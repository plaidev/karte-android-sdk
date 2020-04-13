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

import android.app.Activity
import io.karte.android.core.logger.Logger
import io.karte.android.utilities.ActivityLifecycleCallback
import io.karte.android.visualtracking.VisualTracking

internal class LifecycleHook internal constructor(private val manager: VisualTracking) :
    ActivityLifecycleCallback() {

    override fun onActivityResumed(activity: Activity) {
        try {
            manager.handleLifecycle("android.app.Activity#onResume", activity)
        } catch (e: Exception) {
            Logger.e(LOG_TAG, "Failed to handle lifecycle: android.app.Activity#onResume", e)
        }
    }

    companion object {
        private val LOG_TAG = "Karte.ATLifecycleHook"
    }
}
