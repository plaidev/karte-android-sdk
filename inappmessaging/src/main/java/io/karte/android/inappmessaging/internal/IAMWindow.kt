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

package io.karte.android.inappmessaging.internal

import android.annotation.SuppressLint
import android.app.Activity
import android.view.View
import android.view.ViewGroup
import io.karte.android.core.logger.Logger
import io.karte.android.inappmessaging.InAppMessaging
import io.karte.android.inappmessaging.internal.view.WindowView

private const val LOG_TAG = "Karte.IAMWindow"

@SuppressLint("ViewConstructor")
internal class IAMWindow(val activity: Activity, panelWindowManager: PanelWindowManager) :
    WindowView(activity, panelWindowManager) {
    val isShowing: Boolean
        get() = visibility == VISIBLE && isAttachedToWindow

    // WindowManager への attach 完了後に追加する View
    private var pendingContentView: View? = null

    override fun addView(child: View) {
        if (child.parent != null) {
            Logger.e(LOG_TAG, "webView already has Parent View!")
            (child.parent as ViewGroup).removeView(child)
        }
        this.addView(child, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun show(focus: Boolean, view: View?) {
        setFocus(focus)
        pendingContentView = view
        super.show()
        InAppMessaging.delegate?.onWindowPresented()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // Window の attach 処理中に WebView を追加すると描画されない場合があるため
        // 確実に attach 処理を抜けた後に addView するように post をする
        post {
            if (!isAttachedToWindow) return@post
            val contentView = pendingContentView ?: return@post
            pendingContentView = null
            addView(contentView)
        }
    }

    fun dismiss(withDelay: Boolean) {
        pendingContentView = null
        if (withDelay) {
            postDelayed({
                dismiss(false)
            }, 50)
            return
        }
        super.dismiss()
        removeAllViews()
        InAppMessaging.delegate?.onWindowDismissed()
    }
}
