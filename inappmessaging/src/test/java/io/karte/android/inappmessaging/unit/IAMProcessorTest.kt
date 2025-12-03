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
package io.karte.android.inappmessaging.unit

import android.app.Activity
import com.google.common.truth.Truth.assertThat
import io.karte.android.inappmessaging.internal.IAMProcessor
import io.karte.android.inappmessaging.internal.IAMWebView
import io.karte.android.inappmessaging.internal.IAMWindow
import io.karte.android.inappmessaging.internal.PanelWindowManager
import io.karte.android.test_lib.application
import io.karte.android.test_lib.shadow.CustomShadowWebView
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.justRun
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import io.mockk.verify
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@Suppress("NonAsciiCharacters")
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], shadows = [CustomShadowWebView::class])
class IAMProcessorTest {

    @MockK
    private lateinit var panelWindowManager: PanelWindowManager

    private lateinit var processor: IAMProcessor
    private lateinit var activity: ActivityController<Activity>
    private var isShowing = false

    @Before
    fun init() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockkConstructor(IAMWebView::class, IAMWindow::class)
        // View周りが実際に実行されるとRobolectricのmockがないところでloopしてしまうのでmockする
        every { anyConstructed<IAMWindow>().show() } answers { isShowing = true }
        justRun { anyConstructed<IAMWindow>().addView(any()) }
        justRun { anyConstructed<IAMWindow>().removeAllViews() }
        every { anyConstructed<IAMWindow>().dismiss(any()) } answers { isShowing = false }
        every { anyConstructed<IAMWindow>().isShowing } answers { isShowing }
        // 上のmockでattachできないのでdelayedされたものは無理やり実行する
        every { anyConstructed<IAMWindow>().postDelayed(any(), any()) } answers {
            (firstArg() as Runnable).run()
            true
        }

        processor = IAMProcessor(application(), panelWindowManager, isAutoScreenBoundaryEnabled = true)
        activity = Robolectric.buildActivity(Activity::class.java).create().start()
    }

    @After
    fun teardown() {
        unmockkConstructor(IAMWebView::class, IAMWindow::class)
    }

    private fun resumeActivity() {
        activity.resume()
    }

    private fun pauseActivity() {
        activity.pause()
    }

    private fun openAction() {
        // メッセージの展開はしないでフラグだけ書き換える
        every { anyConstructed<IAMWebView>().visible } returns true
        processor.onWebViewVisible()
    }

    private fun closeAction() {
        every { anyConstructed<IAMWebView>().visible } returns false
        processor.onWebViewInvisible()
    }

    @Test
    fun receive時にはhandleを呼ぶ() {
        // webviewが生成される前にprocessorのメソッドが呼ばれてしまうことがあるので、mockしておく
        justRun { anyConstructed<IAMWebView>().handleResponseData(any()) }

        val message = createMessagePopup()
        processor.handle(message)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        verify(exactly = 1) { anyConstructed<IAMWebView>().handleResponseData(message.string) }
    }

    @Test
    fun pv_idのreset時にはhandleChangePvする() {
        // webviewが生成される前にprocessorのメソッドが呼ばれてしまうことがあるので、mockしておく
        justRun { anyConstructed<IAMWebView>().handleChangePv() }
        justRun { anyConstructed<IAMWebView>().reset(any()) }

        processor.handleChangePv()
        processor.reset(false)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        verify(exactly = 1) { anyConstructed<IAMWebView>().handleChangePv() }
        verify(exactly = 1) { anyConstructed<IAMWebView>().reset(false) }
    }

    @Test
    fun view送信時にはhandleViewする() {
        // webviewが生成される前にprocessorのメソッドが呼ばれてしまうことがあるので、mockしておく
        justRun { anyConstructed<IAMWebView>().handleView(any()) }

        val values = JSONObject()
        processor.handleView(values)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        verify(exactly = 1) { anyConstructed<IAMWebView>().handleView(values) }
    }

    @Test
    fun dismiss時にはresetする() {
        // webviewが生成される前にprocessorのメソッドが呼ばれてしまうことがあるので、mockしておく
        justRun { anyConstructed<IAMWebView>().reset(any()) }

        processor.reset(true)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        verify(exactly = 1) { anyConstructed<IAMWebView>().reset(true) }
    }

    @Test
    fun webviewに接客が追加されたら_Activityが出たときに表示し_接客が閉じられたら非表示する() {
        openAction()
        assertThat(processor.isPresenting).isFalse()

        resumeActivity()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        assertThat(processor.isPresenting).isTrue()

        closeAction()
        assertThat(processor.isPresenting).isFalse()
    }

    @Test
    fun webviewに接客が追加されたら_Activityが出たときに表示し_Activityが閉じられたら非表示する() {
        openAction()
        assertThat(processor.isPresenting).isFalse()

        resumeActivity()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        assertThat(processor.isPresenting).isTrue()

        pauseActivity()
        assertThat(processor.isPresenting).isFalse()
    }

    @Test
    fun Activityが出ているとき_WebViewに接客が追加されたら表示し_接客が閉じられたら非表示する() {
        assertThat(processor.isPresenting).isFalse()

        resumeActivity()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        assertThat(processor.isPresenting).isFalse()

        openAction()
        assertThat(processor.isPresenting).isTrue()

        closeAction()
        assertThat(processor.isPresenting).isFalse()
    }

    @Test
    fun Activityが出ているとき_WebViewに接客が追加されたら表示し_Activityが閉じられたら非表示する() {
        assertThat(processor.isPresenting).isFalse()

        resumeActivity()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        assertThat(processor.isPresenting).isFalse()

        openAction()
        assertThat(processor.isPresenting).isTrue()

        pauseActivity()
        assertThat(processor.isPresenting).isFalse()
    }
}
