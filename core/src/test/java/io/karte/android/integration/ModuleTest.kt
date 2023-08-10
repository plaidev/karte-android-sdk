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
package io.karte.android.integration

import android.app.Activity
import android.content.Intent
import android.net.Uri
import io.karte.android.KarteApp
import io.karte.android.core.library.ActionModule
import io.karte.android.core.library.CommandModule
import io.karte.android.core.library.DeepLinkModule
import io.karte.android.core.library.Library
import io.karte.android.core.library.Module
import io.karte.android.core.library.NotificationModule
import io.karte.android.core.library.TrackModule
import io.karte.android.core.library.UserModule
import io.karte.android.test_lib.RobolectricTestCase
import io.karte.android.test_lib.TrackerRequestDispatcher
import io.karte.android.test_lib.proceedBufferedCall
import io.karte.android.test_lib.setupKarteApp
import io.karte.android.test_lib.tearDownKarteApp
import io.karte.android.tracking.Tracker
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.robolectric.Robolectric

abstract class ModuleTestCase : RobolectricTestCase(), Library {

    lateinit var server: MockWebServer
    private lateinit var dispatcher: TrackerRequestDispatcher
    abstract val mock: Module

    //region Library
    override val name: String = "test_library"
    override val version: String = "0.0.0"
    override val isPublic: Boolean = false

    override fun configure(app: KarteApp) {
        app.register(mock)
    }

    override fun unconfigure(app: KarteApp) {
        app.unregister(mock)
    }
    //endregion

    @Before
    fun setup() {
        server = MockWebServer()
        dispatcher = TrackerRequestDispatcher()
        server.dispatcher = dispatcher
        server.start()

        KarteApp.register(this)
        setupKarteApp(server)
    }

    @After
    fun tearDown() {
        tearDownKarteApp()
        KarteApp.unregister(this)
    }
}

@RunWith(Enclosed::class)
class ModuleTest {
    class ActionModuleTest : ModuleTestCase() {
        override val mock = mockk<ActionModule>(relaxed = true)

        @Test
        fun track受信でreceiveが呼ばれること() {
            verify(exactly = 0) { mock.receive(any(), any()) }

            Tracker.view("test", "テスト")
            proceedBufferedCall()
            // TODO: 起動時イベント等も拾ってしまうからiOSのように抑制すべき
            verify(atLeast = 1) { mock.receive(any(), any()) }
        }

        @Test
        fun viewイベント発火でresetが呼ばれること() {
            verify(exactly = 0) { mock.resetAll() }

            // Tracker.trackではviewイベントでなければ呼ばれない
            Tracker.track("custom_event")
            proceedBufferedCall()
            verify(exactly = 0) { mock.reset() }

            // Tracker.viewの場合
            Tracker.view("test", "テスト")
            proceedBufferedCall()
            verify(exactly = 1) { mock.reset() }

            // Tracker.trackの場合
            Tracker.track(
                "view", mapOf(
                    "view_name" to "test",
                    "title" to "テスト"
                )
            )
            proceedBufferedCall()
            verify(exactly = 2) { mock.reset() }
        }

        @Test
        fun optoutでresetAllが呼ばれること() {
            verify(exactly = 0) { mock.resetAll() }

            KarteApp.optOut()
            verify(exactly = 1) { mock.resetAll() }
        }
    }

    class UserModuleTest : ModuleTestCase() {
        override val mock = mockk<UserModule>(relaxed = true)

        @Test
        fun renewVisitorIdが呼ばれること() {
            verify(exactly = 0) { mock.renewVisitorId(any(), any()) }

            KarteApp.renewVisitorId()
            verify(exactly = 1) { mock.renewVisitorId(any(), any()) }
        }
    }

    class NotificationModuleTest : ModuleTestCase() {
        override val mock = mockk<NotificationModule>(relaxed = true)

        @Test
        fun unsubscribeが呼ばれること() {
            verify(exactly = 0) { mock.unsubscribe() }

            KarteApp.optOut()
            verify(exactly = 1) { mock.unsubscribe() }
        }
    }

    class DeepLinkModuleTest : ModuleTestCase() {
        override val mock = mockk<DeepLinkModule>(relaxed = true)

        private fun launchByDeepLink(uriString: String) {
            Robolectric.buildActivity(
                Activity::class.java,
                Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
            ).create().start().resume()
        }

        @Test
        fun handleが呼ばれること() {
            verify(exactly = 0) { mock.handle(any()) }

            launchByDeepLink("test://aaa")
            // createとstartで2回呼ばれる
            verify(exactly = 2) { mock.handle(any()) }
        }
    }

    class TrackModuleTest : ModuleTestCase() {
        override val mock = mockk<TrackModule>(relaxed = true)

        @Before
        fun start() {
            // relaxedだと他のテストがうまく動かないので全体でmockする
            every { mock.prepare(any()) } returnsArgument 0
        }

        @Test
        fun interceptが毎回呼ばれること() {
            // interceptではrequestをそのまま返す
            every { mock.intercept(any()) } returnsArgument 0

            verify(exactly = 0) { mock.intercept(any()) }

            Tracker.track("test")
            proceedBufferedCall()
            verify(exactly = 1) { mock.intercept(any()) }

            Tracker.track("test")
            proceedBufferedCall()
            verify(exactly = 2) { mock.intercept(any()) }
        }

        @Test
        fun prepareが毎回呼ばれること() {
            // startでmock済み

            verify(exactly = 0) { mock.prepare(any()) }

            Tracker.track("test")
            proceedBufferedCall()
            verify(exactly = 1) { mock.prepare(any()) }

            Tracker.track("test")
            proceedBufferedCall()
            verify(exactly = 2) { mock.prepare(any()) }
        }

        @Test
        fun eventRejectionFilterRulesが一度だけ呼ばれること() {
            every { mock.eventRejectionFilterRules } returns emptyList()
            verify(exactly = 0) { mock.eventRejectionFilterRules }

            Tracker.track("test")
            proceedBufferedCall()
            verify(exactly = 1) { mock.eventRejectionFilterRules }

            // 二回目以降も1度しか呼ばれない。
            Tracker.track("test")
            proceedBufferedCall()
            verify(exactly = 1) { mock.eventRejectionFilterRules }
        }
    }

    class CommandModuleTest : ModuleTestCase() {
        override val mock = mockk<CommandModule>(relaxed = true)

        @Test
        fun validateが毎回呼ばれること() {
            every { mock.validate(any()) } returns false

            verify(exactly = 0) { mock.validate(any()) }

            KarteApp.self.executeCommand(Uri.parse("test://aa"))
            verify(exactly = 1) { mock.validate(any()) }

            KarteApp.self.executeCommand(Uri.parse("test://bb"))
            verify(exactly = 2) { mock.validate(any()) }
        }

        @Test
        fun executeが呼ばれること() {
            val validUri = Uri.parse("test://valid")
            val invalidUri = Uri.parse("test://invalid")
            every { mock.validate(validUri) } returns true
            every { mock.validate(invalidUri) } returns false
            every { mock.execute(any()) } returns null

            verify(exactly = 0) { mock.execute(any()) }

            // invalidなuriでは呼ばれない
            KarteApp.self.executeCommand(invalidUri)
            verify(exactly = 0) { mock.execute(any()) }

            // validなuriでは呼ばれる
            KarteApp.self.executeCommand(validUri)
            verify(exactly = 1) { mock.execute(any()) }
        }
    }
}
