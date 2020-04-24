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
package io.karte.android.visualtracking.unit

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import io.karte.android.RobolectricTestCase
import io.karte.android.visualtracking.internal.TraceBuilder
import net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
import org.json.JSONObject
import org.junit.Test
import org.robolectric.Robolectric

private fun createLinearLayoutWithText(context: Context, text: String): View {
    val contentView = FrameLayout(context)
    val layout = LinearLayout(context).apply {
        addView(TextView(context).apply { this.text = text })
        contentView.addView(View(context))
        contentView.addView(this)
    }
    if (context is Activity) context.setContentView(contentView)
    return layout
}

class TraceBuilderTest : RobolectricTestCase() {
    private val appInfo = JSONObject().put("version_name", "1.5.5")
    private val traceBuilder = TraceBuilder(appInfo)

    @Test
    fun viewを含むaction() {
        val activity = Robolectric.buildActivity(Activity::class.java).get()
        val layout = createLinearLayoutWithText(activity, "hoge")
        val values = traceBuilder.buildTrace("action1", arrayOf(layout, "fuga")).values
        assertThatJson(values).isObject.containsAllEntriesOf(
            mapOf(
                "action" to "action1",
                "target_text" to "hoge",
                "view" to "android.widget.LinearLayout",
                "activity" to "android.app.Activity",
                "action_id" to "android.widget.LinearLayout1android.widget.FrameLayout0android.widget.FrameLayout0com.android.internal.widget.ActionBarOverlayLayout0com.android.internal.policy.DecorView"
            )
        )
        assertThatJson(values).node("app_info.version_name").isString.isEqualTo("1.5.5")
    }

    @Test
    fun viewを含まないaction() {
        val values = traceBuilder.buildTrace("action1", args = arrayOf("hoge")).values
        assertThatJson(values).node("action").isString.isEqualTo("action1")
        assertThatJson(values).node("app_info.version_name").isString.isEqualTo("1.5.5")
        assertThatJson(values).isObject.doesNotContainKeys("target_text", "view", "activity")
    }

    @Test
    fun contextがactivityではないview() {
        val layout = createLinearLayoutWithText(application.applicationContext, "hoge")
        val values = traceBuilder.buildTrace("action1", arrayOf("hoge", layout)).values
        assertThatJson(values).isObject.containsAllEntriesOf(
            mapOf(
                "action" to "action1",
                "target_text" to "hoge",
                "view" to "android.widget.LinearLayout",
                "action_id" to "android.widget.LinearLayout1android.widget.FrameLayout"
            )
        )
        assertThatJson(values).isObject.doesNotContainKey("activity")
        assertThatJson(values).node("app_info.version_name").isString.isEqualTo("1.5.5")
    }

    @Test
    fun lifecycleからtraceを生成() {
        val activity = Robolectric.buildActivity(Activity::class.java).get()
        val values = traceBuilder.buildTrace("onResume", activity).values
        assertThatJson(values).isObject.containsAllEntriesOf(
            mapOf(
                "activity" to "android.app.Activity",
                "action" to "onResume"
            )
        )
        assertThatJson(values).isObject.doesNotContainKeys("view", "target_text")
        assertThatJson(values).node("app_info.version_name").isString.isEqualTo("1.5.5")
    }

    @Test
    fun ListActivityの要素クリックのアクションでは第二引数のviewが使われる() {
        val activity = Robolectric.buildActivity(Activity::class.java).get()
        val layout = createLinearLayoutWithText(activity, "hoge")
        val values = traceBuilder.buildTrace(
            "android.app.ListActivity#onListItemClick", args = arrayOf(
                ListView(activity), layout
            )
        ).values
        assertThatJson(values).isObject.containsAllEntriesOf(
            mapOf(
                "action" to "android.app.ListActivity#onListItemClick",
                "view" to "android.widget.LinearLayout",
                "target_text" to "hoge",
                "activity" to "android.app.Activity",
                "action_id" to "android.widget.LinearLayout1android.widget.FrameLayout0android.widget.FrameLayout0com.android.internal.widget.ActionBarOverlayLayout0com.android.internal.policy.DecorView"
            )
        )
        assertThatJson(values).node("app_info.version_name").isString.isEqualTo("1.5.5")
    }
}
