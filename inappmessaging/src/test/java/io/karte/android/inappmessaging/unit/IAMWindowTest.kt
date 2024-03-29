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
import android.view.KeyEvent
import android.view.WindowManager
import io.karte.android.inappmessaging.internal.IAMProcessor
import io.karte.android.inappmessaging.internal.IAMWebView
import io.karte.android.inappmessaging.internal.IAMWindow
import io.karte.android.inappmessaging.internal.PanelWindowManager
import io.karte.android.test_lib.proceedUiBufferedCall
import io.karte.android.test_lib.shadow.CustomShadowWebView
import io.karte.android.test_lib.shadow.customShadowOf
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24], shadows = [CustomShadowWebView::class])
class IAMWindowTest {

    @MockK
    private lateinit var processor: IAMProcessor
    private lateinit var webView: IAMWebView
    lateinit var activity: ActivityController<Activity>
    private lateinit var view: IAMWindow

    @Before
    fun init() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockKarteApp()

        activity = Robolectric.buildActivity(Activity::class.java).create().start().visible()
        webView =
            IAMWebView(
                activity.get().applicationContext,
                processor
            )
        webView.visible = true
        view = IAMWindow(activity.get(), PanelWindowManager())
        view.addView(webView)
    }

    @After
    fun teardown() {
        unmockKarteApp()
    }

    @Test
    fun 通常のshow_dismiss() {
        view.show()
        proceedUiBufferedCall()

        Assert.assertEquals(true, view.isShowing)
        view.dismiss(false)
        // dismiss後もviewのvisibilityは変化しない
    }

    @Test
    fun softInputAdjustNothingのとき() {
        activity.get().window.attributes.softInputMode =
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        view.show()
        proceedUiBufferedCall()

        Assert.assertEquals(true, view.isShowing)
        view.dismiss(false)
        // dismiss後もviewのvisibilityは変化しない
    }

    @Test
    fun backボタンが押された時WebViewにイベントがいくか() {
        val shadowWebView = customShadowOf(webView)
        Assert.assertEquals(false, shadowWebView.keyEventCalled)

        view.show()
        val result = view.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK))
        Assert.assertEquals(true, result)
        Assert.assertEquals(true, shadowWebView.wasKeyEventCalled())
    }
}
