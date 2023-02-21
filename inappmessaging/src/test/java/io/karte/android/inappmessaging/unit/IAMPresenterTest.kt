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

import io.karte.android.inappmessaging.internal.IAMPresenter
import io.karte.android.inappmessaging.internal.MessageModel
import io.karte.android.inappmessaging.internal.Window
import io.karte.android.test_lib.shadow.CustomShadowWebView
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Suppress("NonAsciiCharacters")
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], shadows = [CustomShadowWebView::class])
class IAMPresenterTest {

    @get:Rule
    var folder = TemporaryFolder()

    @MockK
    private lateinit var viewMock: Window

    @MockK
    private lateinit var webView: MessageModel.MessageView

    private lateinit var presenter: IAMPresenter

    @Before
    fun init() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        presenter = IAMPresenter(viewMock, webView)
    }

    @Test
    fun destroyが呼ばれるとviewをdestroyする() {
        presenter.destroy()

        verify(exactly = 1) { viewMock.destroy() }
    }

    @Test
    fun 初期化時に表示する() {
        verify(exactly = 1) { viewMock.presenter = presenter }
        verify(exactly = 1) { webView.adapter = presenter }
    }

    @Test
    fun addMessage時にViewに通知する() {
        presenter.addMessage(createMessagePopup())
        verify(exactly = 1) { webView.notifyChanged() }
    }

    @Test
    fun QueueはFIFOで_messageがない時はdequeueはnullを返す() {
        Assert.assertNull(presenter.dequeue())

        presenter.addMessage(createMessagePopup("campaign1"))
        presenter.addMessage(createMessagePopup("campaign2"))
        Assert.assertEquals(createMessagePopup("campaign1").string, presenter.dequeue()?.string)
        Assert.assertEquals(createMessagePopup("campaign2").string, presenter.dequeue()?.string)
        Assert.assertEquals(null, presenter.dequeue())

        Assert.assertNull(presenter.dequeue())
    }
}
