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
package io.karte.android.visualtracking.internal.tracing

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.view.ViewTreeObserver
import androidx.annotation.UiThread
import io.karte.android.visualtracking.ImageProvider
import org.json.JSONObject

internal typealias BitmapCallback = (bitmap: Bitmap?) -> Unit

@UiThread
internal class Trace internal constructor(
    private val view: View?,
    val values: JSONObject,
    private val imageProvider: ImageProvider? = null
) {

    internal fun getBitmapIfNeeded(callback: BitmapCallback) {
        if (imageProvider != null) {
            callback.invoke(imageProvider.image())
            return
        }

        if (view == null) {
            callback.invoke(null)
            return
        }

        if (view.width == 0 || view.height == 0) {
            view.viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (view.width > 0 && view.height > 0) {
                        callback.invoke(getBitmapInternal(view))
                        view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                }
            })
        } else {
            callback.invoke(getBitmapInternal(view))
        }
    }

    private fun getBitmapInternal(view: View): Bitmap {
        var backgroundColor = Color.TRANSPARENT
        var activity: Activity? = null
        if (view.context is Activity) {
            activity = view.context as Activity
        }
        if (activity != null) {
            val array =
                activity.theme.obtainStyledAttributes(intArrayOf(android.R.attr.colorBackground))
            backgroundColor = array.getColor(0, Color.TRANSPARENT)
            array.recycle()
        }
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(backgroundColor)
        view.draw(canvas)
        return bitmap
    }
}
