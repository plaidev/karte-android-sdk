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
package io.karte.android.visualtracking.internal

import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.view.Window
import android.widget.TextView

private val NUM_MATCHER = "[0-9]+".toRegex()

/**
 * actionIdからView階層のパスを示すindex配列を返す。（例: View1View0からは、[0,1]が返される）
 */
internal fun viewPathIndices(actionId: String?): IntArray? {
    return actionId?.let { id ->
        NUM_MATCHER.findAll(id).mapNotNull { it.value.toIntOrNull() }
            .toList().reversed().toIntArray()
    }
}

/**
 * View階層のパス情報でwindowのchildを探索して見つかったViewを返す
 */
internal fun viewFrom(viewPathIndices: IntArray?, window: Window?): View? {
    if (viewPathIndices == null || window == null) return null

    var target: View? = window.decorView
    for (i in viewPathIndices) {
        if (target is ViewGroup) {
            if (i > target.childCount - 1) return null
            target = target.getChildAt(i)
        }
    }
    return target
}

/**
 * Viewから適切なテキストを探して返す
 */
internal fun getTargetText(view: View): String? {
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            val text = getTargetText(view.getChildAt(i))
            if (text != null) return text
        }
    }
    if (view is TextView) {
        val text = view.text
        if (text != null) {
            return text.toString()
        }
    }
    return null
}

/**
 * Viewの階層情報を連結してactionIdとして返す。
 */
internal fun getActionId(view: View?): String? {
    if (view == null) return null

    var target = view
    val sb = StringBuilder()

    while (target != null) {
        sb.append(target.javaClass.name)
        val parent = target.parent

        if (parent is ViewGroup) {
            var i = 0
            while (i < parent.childCount) {
                val v = parent.getChildAt(i)
                if (v === target)
                    sb.append(i)
                i++
            }
        }

        target = when (parent) {
            is View -> parent
            is ViewParent -> {
                sb.append(parent.javaClass.name)
                null
            }
            else -> null
        }
    }
    return sb.toString()
}
