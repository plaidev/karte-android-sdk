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
package io.karte.android.notifications.integration

import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic

fun mockFirebaseMessaging(mockedToken: String?) {
    mockkStatic(FirebaseMessaging::class)
    val slot = slot<OnCompleteListener<String>>()
    val mockedInstance = mockk<FirebaseMessaging> {
        every { token } returns mockk {
            every { addOnCompleteListener(capture(slot)) } answers {
                val result: Task<String> = if (mockedToken != null) {
                    mockk {
                        every { isSuccessful } returns true
                        every { result } returns mockedToken
                    }
                } else {
                    mockk { every { isSuccessful } returns false }
                }
                slot.captured.onComplete(result)
                result
            }
        }
    }
    every { FirebaseMessaging.getInstance() } returns mockedInstance
}

fun unmockFirebaseMessaging() {
    unmockkStatic(FirebaseMessaging::class)
}
