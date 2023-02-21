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
package io.karte.android.test_lib.shadow

import android.view.KeyEvent
import android.webkit.WebView
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowWebView
import org.robolectric.util.ReflectionHelpers

@Implements(value = WebView::class)
class CustomShadowWebView : ShadowWebView() {
    @RealObject
    private lateinit var realWebView: WebView

    val loadedUrls: MutableList<String> = mutableListOf()
    override fun loadUrl(url: String?) {
        current = this
        super.loadUrl(url)
        pushEntryToHistory(url)
        loadedUrls.add(url!!)
    }

    fun resetLoadedUrls() {
        current = this
        loadedUrls.clear()
    }

    var keyEventCalled = false

    @Implementation
    fun dispatchKeyEvent(event: KeyEvent): Boolean {
        keyEventCalled = true
        return Shadow.directlyOn(realWebView, WebView::class.java, "dispatchKeyEvent", ReflectionHelpers.ClassParameter.from(KeyEvent::class.java, event))
    }

    fun wasKeyEventCalled(): Boolean {
        return keyEventCalled
    }

    companion object {
        var current: CustomShadowWebView? = null
            private set

        fun teardown() {
            current?.resetLoadedUrls()
            current = null
        }
    }
}

fun customShadowOf(webView: WebView): CustomShadowWebView = shadowOf(webView) as CustomShadowWebView
