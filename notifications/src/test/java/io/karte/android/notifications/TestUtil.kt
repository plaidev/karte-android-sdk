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
package io.karte.android.notifications

import android.app.NotificationManager
import io.karte.android.notifications.internal.THREAD_NAME
import io.karte.android.test_lib.RobolectricTestCase
import io.karte.android.test_lib.getThreadByName
import io.karte.android.test_lib.proceedBufferedCall
import org.robolectric.Shadows

fun NotificationManager.setPermission(enabled: Boolean) {
    Shadows.shadowOf(this).setNotificationsEnabled(enabled)
}

val RobolectricTestCase.manager: NotificationManager
    get() = application.getSystemService(NotificationManager::class.java)

fun proceedBufferedThreads() {
    getThreadByName(THREAD_NAME)?.join()
    proceedBufferedCall()
}
