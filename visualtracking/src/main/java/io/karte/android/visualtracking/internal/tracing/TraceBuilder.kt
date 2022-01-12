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
import androidx.annotation.UiThread
import com.google.android.material.tabs.TabLayout
import io.karte.android.visualtracking.Action
import io.karte.android.visualtracking.internal.HookTargetMethodFromDynamicInvoke
import io.karte.android.visualtracking.internal.getActionId
import io.karte.android.visualtracking.internal.getTargetText
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

    @Throws(JSONException::class)
    fun buildTrace(action: Action): Trace {
        val values = JSONObject()
            .put("app_info", this.appInfo)
            .put("action", action.action)
            .putOpt("target_text", action.targetText)
            .putOpt("action_id", action.actionId)
        return Trace(null, values, action.imageProvider)
    }

    private fun getContentView(activity: Activity): View {
        return activity.findViewById(android.R.id.content)
    }

    private fun getView(actionName: String, args: Array<Any>): View? {
        // TODO: abstraction.
        return when (actionName) {
            "android.app.ListActivity#onListItemClick",
            "android.widget.AdapterView\$OnItemClickListener#onItemClick" ->
                args[1] as View
            "com.google.android.material.tabs.TabLayout\$OnTabSelectedListener#onTabSelected",
            "android.support.design.widget.TabLayout\$OnTabSelectedListener#onTabSelected" -> {
                val tab = args[0] as TabLayout.Tab
                @Suppress("INACCESSIBLE_TYPE")
                tab.customView ?: tab.view
            }
            HookTargetMethodFromDynamicInvoke.VIEW_CLICK.actionName -> {
                args.lastOrNull { it is View } as? View
            }
            HookTargetMethodFromDynamicInvoke.ADAPTER_VIEW_ITEM_CLICK.actionName -> {
                args.lastOrNull { it is View } as? View
            }
            else -> args.firstOrNull { it is View } as? View
        }
    }

    private fun getActivity(view: View): Activity? {
        return if (view.context is Activity) view.context as Activity else null
    }
}
