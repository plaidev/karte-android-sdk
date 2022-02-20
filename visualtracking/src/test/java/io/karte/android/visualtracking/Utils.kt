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
package io.karte.android.visualtracking

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import io.karte.android.toJSONArray
import io.karte.android.visualtracking.internal.PairingManager
import io.karte.android.visualtracking.internal.tracking.DefinitionList
import io.mockk.every
import io.mockk.mockk
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ExecutorService
import java.util.concurrent.ScheduledExecutorService

// Ugly hack: Because Robolectric doesn't handle ExecutorService, inject executor that run command immediately.
// http://robolectric.org/best-practices/
fun injectDirectExecutorServiceToAutoTrackModules() {
    val executorService = mockk<ExecutorService>()
    every { executorService.execute(any()) } answers { (firstArg() as Runnable).run() }
    val scheduledExecutorService = mockk<ScheduledExecutorService>(relaxed = true)
    every { scheduledExecutorService.execute(any()) } answers { (firstArg() as Runnable).run() }

    VisualTracking::class.java.getDeclaredField("executor").run {
        isAccessible = true
        set(VisualTracking.self, executorService)
    }

    PairingManager::class.java.getDeclaredField("pollingExecutor").run {
        isAccessible = true
        set(VisualTracking.self?.pairingManager, scheduledExecutorService)
    }

    PairingManager::class.java.getDeclaredField("traceSendExecutor").run {
        isAccessible = true
        set(VisualTracking.self?.pairingManager, executorService)
    }
}

fun createLinearLayoutWithText(context: Context, text: String): View {
    val contentView = FrameLayout(context)
    val layout = LinearLayout(context).apply {
        addView(TextView(context).apply { this.text = text })
        contentView.addView(View(context))
        contentView.addView(this)
    }
    if (context is Activity) context.setContentView(contentView)
    return layout
}

fun condition(field: String, comparator: String, value: String): JSONObject {
    return JSONObject().put(field, JSONObject().put(comparator, value))
}

fun andCondition(vararg conditions: JSONObject): JSONObject {
    return JSONObject().put("\$and", conditions.toJSONArray())
}

fun trigger(
    vararg conditions: JSONObject,
    fields: JSONObject? = JSONObject(),
    dynamicFields: JSONArray? = JSONArray()
): JSONObject {
    return JSONObject()
        .put("condition", andCondition(*conditions))
        .put("fields", fields)
        .put("dynamic_fields", dynamicFields)
}

fun definition(eventName: String, vararg triggers: JSONObject): JSONObject {
    return JSONObject().put("event_name", eventName).put("triggers", triggers.toJSONArray())
}

fun autoTrackDefinition(
    vararg definitions: JSONObject,
    lastModified: Long = 0,
    status: String = "modified"
): JSONObject {
    return JSONObject()
        .put("definitions", definitions.toJSONArray())
        .put("last_modified", lastModified)
        .put("status", status)
}

internal fun buildDefinitionList(vararg definitions: JSONObject): DefinitionList? {
    return DefinitionList.buildIfNeeded(autoTrackDefinition(*definitions))
}
