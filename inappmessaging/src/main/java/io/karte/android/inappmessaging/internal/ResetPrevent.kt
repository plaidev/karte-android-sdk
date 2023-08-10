package io.karte.android.inappmessaging.internal

import android.app.Activity

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

private const val PREVENT_RESET_KEY = "krt_iam_prevent_reset"

internal object ResetPrevent {
    internal fun enablePreventResetFlag(activity: Activity?) {
        activity?.intent?.putExtra(PREVENT_RESET_KEY, true)
    }

    internal fun isPreventReset(activity: Activity): Boolean {
        var isPreventReset = false
        activity.intent?.let { intent ->
            isPreventReset =
                intent.getBooleanExtra(PREVENT_RESET_KEY, false)
            intent.removeExtra(PREVENT_RESET_KEY)
        }
        return isPreventReset
    }
}
