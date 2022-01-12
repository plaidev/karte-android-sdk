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
import android.view.View
import android.widget.ListView
import io.karte.android.RobolectricTestCase
import io.karte.android.visualtracking.BasicAction
import io.karte.android.visualtracking.createLinearLayoutWithText
import io.karte.android.visualtracking.internal.HookTargetMethodFromDynamicInvoke
import io.karte.android.visualtracking.internal.getActionId
import io.karte.android.visualtracking.internal.getTargetText
import io.karte.android.visualtracking.internal.tracing.TraceBuilder
import io.karte.android.visualtracking.internal.viewFrom
import io.karte.android.visualtracking.internal.viewPathIndices
import net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.robolectric.Robolectric

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
                "action_id" to
                    "android.widget.LinearLayout1" +
                    "android.widget.FrameLayout0" +
                    "android.widget.FrameLayout0" +
                    "com.android.internal.widget.ActionBarOverlayLayout0" +
                    "com.android.internal.policy.DecorView"
            )
        )
        assertThatJson(values).node("app_info.version_name").isString.isEqualTo("1.5.5")
    }

    @Test
    fun viewPathからのview生成とtargetText取得() {
        val activity = Robolectric.buildActivity(Activity::class.java).get()
        val layout = createLinearLayoutWithText(activity, "hoge")
        val actionId = getActionId(layout)
        val viewPath = viewPathIndices(actionId)
        val view = viewFrom(viewPath, activity.window)
        val targetText = getTargetText(view!!)
        Assert.assertEquals(targetText, "hoge")
        Assert.assertEquals(actionId, getActionId(view))
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
    fun actionIdを指定してtraceを生成() {
        val action = BasicAction("touch", "actionId", "target_text")
        val values = traceBuilder.buildTrace(action).values
        assertThatJson(values).isObject.containsAllEntriesOf(
            mapOf(
                "action_id" to "actionId",
                "target_text" to "target_text",
                "action" to "touch"
            )
        )
        assertThatJson(values).isObject.doesNotContainKeys("view", "activity")
        assertThatJson(values).node("app_info.version_name").isString.isEqualTo("1.5.5")
    }

    @Test
    fun actionIdからview階層のpathを示すindex配列を生成() {
        val simpleActionId = "View0View11View2View1"
        val action = BasicAction("touch", simpleActionId, "target_text")
        val viewPathIndices = viewPathIndices(action.actionId)
        Assert.assertArrayEquals(viewPathIndices, intArrayOf(1, 2, 11, 0))

        val complexActionId = "android.widget.LinearLayout7android.widget.ListView5" +
            "android.widget.LinearLayout1android.widget.LinearLayout0androidx.appcompat.widget.ContentFrameLayout1" +
            "androidx.appcompat.widget.FitWindowsLinearLayout0android.widget.FrameLayout1android.widget.LinearLayout0" +
            "com.android.internal.policy.DecorViewandroid.view.ViewRootImpl"
        val action2 = BasicAction("touch", complexActionId, "target_text")
        val viewPathIndices2 = viewPathIndices(action2.actionId)
        Assert.assertArrayEquals(viewPathIndices2, intArrayOf(0, 1, 0, 1, 0, 1, 5, 7))

        val action3 = BasicAction("touch", null, null)
        val viewPathIndices3 = viewPathIndices(action3.actionId)
        Assert.assertEquals(viewPathIndices3, null)
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
                "action_id" to
                    "android.widget.LinearLayout1" +
                    "android.widget.FrameLayout0" +
                    "android.widget.FrameLayout0" +
                    "com.android.internal.widget.ActionBarOverlayLayout0" +
                    "com.android.internal.policy.DecorView"
            )
        )
        assertThatJson(values).node("app_info.version_name").isString.isEqualTo("1.5.5")
    }

    @Test
    fun dynamicinvokeでトリガーされるViewクリックアクションは引数末尾のviewが使われる() {
        val activity = Robolectric.buildActivity(Activity::class.java).get()
        val layout = createLinearLayoutWithText(activity, "hoge")
        val values = traceBuilder.buildTrace(
            HookTargetMethodFromDynamicInvoke.VIEW_CLICK.actionName, args = arrayOf(
                ListView(activity), View(activity), View(activity), layout, "string"
            )
        ).values
        assertThatJson(values).isObject.containsAllEntriesOf(
            mapOf(
                "action" to "android.view.View#performClick",
                "view" to "android.widget.LinearLayout",
                "target_text" to "hoge",
                "activity" to "android.app.Activity",
                "action_id" to
                    "android.widget.LinearLayout1" +
                    "android.widget.FrameLayout0" +
                    "android.widget.FrameLayout0" +
                    "com.android.internal.widget.ActionBarOverlayLayout0" +
                    "com.android.internal.policy.DecorView"
            )
        )
        assertThatJson(values).node("app_info.version_name").isString.isEqualTo("1.5.5")
    }

    @Test
    fun dynamicinvokeでトリガーされるItemクリックアクションは引数末尾のviewが使われる() {
        val activity = Robolectric.buildActivity(Activity::class.java).get()
        val layout = createLinearLayoutWithText(activity, "hoge")
        val values = traceBuilder.buildTrace(
            HookTargetMethodFromDynamicInvoke.ADAPTER_VIEW_ITEM_CLICK.actionName, args = arrayOf(
                ListView(activity), View(activity), View(activity), layout, "string"
            )
        ).values
        assertThatJson(values).isObject.containsAllEntriesOf(
            mapOf(
                "action" to "android.widget.AdapterView#performItemClick",
                "view" to "android.widget.LinearLayout",
                "target_text" to "hoge",
                "activity" to "android.app.Activity",
                "action_id" to
                    "android.widget.LinearLayout1" +
                    "android.widget.FrameLayout0" +
                    "android.widget.FrameLayout0" +
                    "com.android.internal.widget.ActionBarOverlayLayout0" +
                    "com.android.internal.policy.DecorView"
            )
        )
        assertThatJson(values).node("app_info.version_name").isString.isEqualTo("1.5.5")
    }
}
