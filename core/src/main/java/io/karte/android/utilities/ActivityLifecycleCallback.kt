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
package io.karte.android.utilities

import android.app.Activity
import android.app.Application
import android.os.Bundle

/** [Application.ActivityLifecycleCallbacks] の空実装の抽象クラスです。*/
abstract class ActivityLifecycleCallback : Application.ActivityLifecycleCallbacks {

    /** @suppress */
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
    }

    /** @suppress */
    override fun onActivityStarted(activity: Activity) {
    }

    /** @suppress */
    override fun onActivityResumed(activity: Activity) {
    }

    /** @suppress */
    override fun onActivityPaused(activity: Activity) {
    }

    /** @suppress */
    override fun onActivityStopped(activity: Activity) {
    }

    /** @suppress */
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    /** @suppress */
    override fun onActivityDestroyed(activity: Activity) {
    }
}
