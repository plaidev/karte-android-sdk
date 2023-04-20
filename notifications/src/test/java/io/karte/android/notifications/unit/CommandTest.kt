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
package io.karte.android.notifications.unit

import android.app.Activity
import android.content.Intent
import android.net.Uri
import io.karte.android.notifications.Notifications
import io.karte.android.notifications.internal.command.RegisterPushCommandExecutor
import io.karte.android.test_lib.RobolectricTestCase
import io.karte.android.test_lib.setupKarteApp
import io.karte.android.test_lib.tearDownKarteApp
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.ref.WeakReference
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class CommandTest : RobolectricTestCase() {
    private val testApiKey = "aWgSxztqGSpKN0otg5w1ruUk8kTBF5vb"
    private lateinit var notificationsMock: Notifications
    private lateinit var commandExecutor: RegisterPushCommandExecutor

    @Before
    fun setup() {
        setupKarteApp()
        notificationsMock = spyk(Notifications.self!!, recordPrivateCalls = true)
        commandExecutor = spyk(recordPrivateCalls = true)
        notificationsMock.app.register(commandExecutor)
        Notifications.self = notificationsMock
    }

    @After
    fun tearDown() {
        tearDownKarteApp()
        clearMocks(notificationsMock, commandExecutor)
        notificationsMock.app.unregister(commandExecutor)
    }

    @Test
    fun testCommandExecute() {
        executeCommand(Uri.parse("invalid-uri"))
        verify(exactly = 1) { commandExecutor.validate(any()) }
        verify(exactly = 0) { commandExecutor.execute(any()) }

        // uriが条件にあえば実行される
        val intent = executeCommand(Uri.parse("krt-$testApiKey://register-push"))
        verify(exactly = 2) { commandExecutor.validate(any()) }
        verify(exactly = 1) { commandExecutor.execute(any()) }
        verify(exactly = 0) { commandExecutor["requestPermission"](any<Activity>()) }
        assertNull(intent)

        // 実際に表示されるのはactivityが存在する時のみ
        every { notificationsMock.currentActivity } returns WeakReference(Activity())
        executeCommand(Uri.parse("krt-$testApiKey://register-push"))
        verify(exactly = 3) { commandExecutor.validate(any()) }
        verify(exactly = 2) { commandExecutor.execute(any()) }
        verify(exactly = 1) { commandExecutor["requestPermission"](any<Activity>()) }
    }

    @Test
    fun testCommandWithDelay() {
        executeCommand(Uri.parse("invalid-uri"), true)
        verify(exactly = 1) { commandExecutor.validate(any()) }
        verify(exactly = 0) { commandExecutor.execute(any(), true) }

        // uriが条件にあえば実行される
        val intent = executeCommand(Uri.parse("krt-$testApiKey://register-push"), true)
        verify(exactly = 2) { commandExecutor.validate(any()) }
        verify(exactly = 1) { commandExecutor.execute(any(), true) }
        verify(exactly = 0) { commandExecutor["requestPermission"](any<Activity>()) }
        assertNull(intent)

        // 遅延実行時はactivityが存在してもアラートは表示されない
        every { notificationsMock.currentActivity } returns WeakReference(Activity())
        executeCommand(Uri.parse("krt-$testApiKey://register-push"), true)
        verify(exactly = 3) { commandExecutor.validate(any()) }
        verify(exactly = 2) { commandExecutor.execute(any(), true) }
        verify(exactly = 0) { commandExecutor["requestPermission"](any<Activity>()) }
    }

    private fun executeCommand(uri: Uri, isDelay: Boolean = false): Intent? {
        return Notifications.self?.app?.executeCommand(uri, isDelay)?.filterIsInstance<Intent>()
            ?.firstOrNull()
    }
}
