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
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.widget.TextView
import androidx.annotation.UiThread
import com.google.android.material.tabs.TabLayout
import org.json.JSONException
import org.json.JSONObject

@UiThread
internal class TraceBuilder(private val appInfo: JSONObject) {

    @Throws(JSONException::class)
    fun buildTrace(actionName: String, args: Array<Any>): Trace {

        val jsonObject = JSONObject()
            .put("action", actionName)
            .put("app_info", appInfo)
        val view = getView(actionName, args) ?: return Trace(null, jsonObject)

        jsonObject.put("view", view.javaClass.name)
            .putOpt("target_text", getTargetText(view))
            .putOpt("action_id", getActionId(view))
        val activity = getActivity(view) ?: return Trace(view, jsonObject)

        jsonObject.put("activity", activity.javaClass.name)

        return Trace(view, jsonObject)
    }

    @Throws(JSONException::class)
    fun buildTrace(actionName: String, activity: Activity): Trace {
        val values = JSONObject()
            .put("app_info", this.appInfo)
            .put("action", actionName)
            .put("activity", activity.javaClass.name)

        return Trace(getContentView(activity), values)
    }

    private fun getContentView(activity: Activity): View {
        return activity.findViewById(android.R.id.content)
    }

    private fun getView(actionName: String, args: Array<Any>): View? {
        // TODO: abstraction.
        when (actionName) {
            "android.app.ListActivity#onListItemClick",
            "android.widget.AdapterView\$OnItemClickListener#onItemClick" ->
                return args[1] as View
            "com.google.android.material.tabs.TabLayout\$OnTabSelectedListener#onTabSelected",
            "android.support.design.widget.TabLayout\$OnTabSelectedListener#onTabSelected" -> {
                val tab = args[0] as TabLayout.Tab
                @Suppress("INACCESSIBLE_TYPE")
                return tab.customView ?: tab.view
            }
        }

        for (arg in args) {
            if (arg is View) {
                return arg
            }
        }
        return null
    }

    private fun getActivity(view: View): Activity? {
        return if (view.context is Activity) view.context as Activity else null
    }

    private fun getTargetText(view: View): String? {
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

    private fun getActionId(view: View?): String? {
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
}
