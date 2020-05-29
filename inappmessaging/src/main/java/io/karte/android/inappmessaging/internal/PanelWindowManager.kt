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
package io.karte.android.inappmessaging.internal

import android.os.Build
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.TYPE_APPLICATION_PANEL
import android.widget.PopupWindow
import androidx.annotation.RequiresApi
import io.karte.android.core.logger.Logger
import java.lang.ref.WeakReference
import java.util.ArrayList

internal class PanelWindowManager {
    private val LOG_TAG = "Karte.IAMPWManager"

    private var lastActionDownWindow: BaseWindowWrapper? = null
    private val windows = ArrayList<BaseWindowWrapper>()

    fun registerPopupWindow(popupWindow: PopupWindow) {
        Logger.i(LOG_TAG, "registerPopupWindow: $popupWindow")
        windows.add(0, PopupWindowWrapper(popupWindow))
    }

    fun registerWindow(window: Window) {
        Logger.i(LOG_TAG, "registerWindow: $window")
        windows.add(0, WindowWrapper(window))
    }

    fun dispatchTouch(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN) {
            lastActionDownWindow?.let {
                if (!it.hasStaleReference()) {
                    it.dispatchTouchForce(event)
                    return true
                }
            }
            return false
        }

        lastActionDownWindow = null
        val iterator = windows.iterator()
        while ((iterator as Iterator<*>).hasNext()) {
            val wrapper = iterator.next()
            if (wrapper.hasStaleReference()) {
                iterator.remove()
                continue
            }

            if (wrapper.dispatchTouch(event)) {
                lastActionDownWindow = wrapper
                return true
            }
        }
        return false
    }

    internal interface BaseWindowWrapper {
        fun hasStaleReference(): Boolean

        fun dispatchTouch(event: MotionEvent): Boolean

        fun dispatchTouchForce(event: MotionEvent)
    }

    internal class WindowWrapper(windowRef: Window) : BaseWindowWrapper {
        private val windowRef: WeakReference<Window> = WeakReference(windowRef)
        private val locationOnScreen = IntArray(2)

        override fun hasStaleReference(): Boolean {
            return windowRef.get() == null
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        override fun dispatchTouch(event: MotionEvent): Boolean {
            val window = windowRef.get()
            if (!isActivePanel(window)) return false

            val params = window!!.attributes
            if (params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE != 0)
                return false

            var shouldDispatch = false
            if (params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL != 0) {
                shouldDispatch = true
            } else {
                val x = event.x.toInt()
                val y = event.y.toInt()
                val view = window.peekDecorView()
                view.getLocationOnScreen(locationOnScreen)
                if (x >= locationOnScreen[0] && x <= locationOnScreen[0] + view.width && y >= locationOnScreen[1] && y <= locationOnScreen[1] + view.height) {
                    shouldDispatch = true
                }
            }
            if (shouldDispatch) {
                event.offsetLocation(
                    (-locationOnScreen[0]).toFloat(),
                    (-locationOnScreen[1]).toFloat()
                )
                window.injectInputEvent(event)
                return true
            }
            return false
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        override fun dispatchTouchForce(event: MotionEvent) {
            val window = windowRef.get()
            if (!isActivePanel(window)) return

            event.offsetLocation((-locationOnScreen[0]).toFloat(), (-locationOnScreen[1]).toFloat())
            window!!.injectInputEvent(event)
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        private fun isActivePanel(window: Window?): Boolean {
            if (window == null) return false
            if (window.attributes.type != TYPE_APPLICATION_PANEL) return false

            val view = window.peekDecorView() ?: return false
            return if (view.visibility != View.VISIBLE) false else view.isAttachedToWindow
        }
    }

    internal class PopupWindowWrapper(popupWindow: PopupWindow) : BaseWindowWrapper {
        private val popupWindowRef: WeakReference<PopupWindow> = WeakReference(popupWindow)
        private val locationOnScreen = IntArray(2)

        private fun isActivePanel(popupWindow: PopupWindow?): Boolean {
            if (popupWindow == null) return false
            if (!popupWindow.isShowing) return false
            if (popupWindow.contentView == null) return false

            return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) true else popupWindow.windowLayoutType == TYPE_APPLICATION_PANEL
        }

        override fun hasStaleReference(): Boolean {
            return popupWindowRef.get() == null
        }

        override fun dispatchTouch(event: MotionEvent): Boolean {
            val popupWindow = popupWindowRef.get()
            if (!(isActivePanel(popupWindow) && popupWindow!!.isTouchable)) {
                return false
            }

            val view = popupWindow.contentView.rootView

            var shouldDispatch = false
            val touchX = event.x.toInt()
            val touchY = event.y.toInt()
            view.getLocationOnScreen(locationOnScreen)
            if (popupWindow.isOutsideTouchable) {
                shouldDispatch = true
            } else {
                if (touchX >= locationOnScreen[0] && touchX <= locationOnScreen[0] + view.width && touchY >= locationOnScreen[1] && touchY <= locationOnScreen[1] + view.height) {
                    shouldDispatch = true
                }
            }

            if (shouldDispatch) {
                event.offsetLocation(
                    (-locationOnScreen[0]).toFloat(),
                    (-locationOnScreen[1]).toFloat()
                )
                view.dispatchTouchEvent(event)
                return true
            } else {
                return false
            }
        }

        override fun dispatchTouchForce(event: MotionEvent) {
            val popupWindow = popupWindowRef.get()
            if (!(isActivePanel(popupWindow) && popupWindow!!.isTouchable)) {
                return
            }

            val view = popupWindow.contentView.rootView
            view.getLocationOnScreen(locationOnScreen)
            event.offsetLocation((-locationOnScreen[0]).toFloat(), (-locationOnScreen[1]).toFloat())

            view.dispatchTouchEvent(event)
        }
    }
}
