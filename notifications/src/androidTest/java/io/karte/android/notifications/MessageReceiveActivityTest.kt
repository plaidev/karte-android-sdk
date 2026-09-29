//
//  Copyright 2026 PLAID, Inc.
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

package io.karte.android.notifications

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.karte.android.notifications.internal.wrapper.ACTION_KARTE_IGNORED
import io.karte.android.notifications.internal.wrapper.EventType
import io.karte.android.notifications.internal.wrapper.KEY_PUSH_NOTIFICATION_FLAG
import io.karte.android.notifications.test.NotificationClickActivityLaunchRule
import io.karte.android.notifications.test.NotificationTestTargetActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MessageReceiveActivityTest {

    @get:Rule
    val activityLaunchRule = NotificationClickActivityLaunchRule()

    @Test
    fun onCreate_whenDeeplinkTargetCanBeResolved_launchesTargetActivity() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = clickIntentWithDeeplink(context, DEEPLINK_URI)

        context.startActivity(intent)

        activityLaunchRule.assertOnlyTargetLaunched()
        activityLaunchRule.assertMessageReceiveActivityFinished()
    }

    @Test
    fun onCreate_whenDeeplinkTargetCannotBeResolved_launchesAppLauncher() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = clickIntentWithDeeplink(context, UNRESOLVABLE_DEEPLINK_URI)

        context.startActivity(intent)

        activityLaunchRule.assertOnlyAppLauncherLaunched()
        activityLaunchRule.assertMessageReceiveActivityFinished()
    }

    @Test
    fun onCreate_whenTargetActivityCanBeResolved_launchesTargetActivity() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val componentName = context.componentNameFor(TARGET_ACTIVITY)
        val intent = clickIntentWithComponentName(context, componentName)

        context.startActivity(intent)

        activityLaunchRule.assertOnlyTargetLaunched()
        activityLaunchRule.assertMessageReceiveActivityFinished()
    }

    @Test
    fun onCreate_whenTargetActivityCannotBeResolved_launchesAppLauncher() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val componentName = context.componentNameFor(UNRESOLVABLE_TARGET_ACTIVITY)
        val intent = clickIntentWithComponentName(context, componentName)

        context.startActivity(intent)

        activityLaunchRule.assertOnlyAppLauncherLaunched()
        activityLaunchRule.assertMessageReceiveActivityFinished()
    }

    @Test
    fun onCreate_whenIgnoreEvent_doesNotLaunchActivity() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val ignoreIntent = ignoreIntent(context)

        context.startActivity(ignoreIntent)

        activityLaunchRule.assertNoKnownActivityCreatedWithin()
        activityLaunchRule.assertMessageReceiveActivityFinished()
    }

    private fun clickIntentWithComponentName(context: Context, componentName: String): Intent =
        clickIntent(context).apply {
            putExtra(EXTRA_COMPONENT_NAME, componentName)
        }

    @SuppressLint("UseKtx")
    private fun clickIntentWithDeeplink(context: Context, uri: String): Intent = clickIntent(context).apply {
        action = Intent.ACTION_VIEW
        data = Uri.parse(uri)
    }

    private fun clickIntent(context: Context): Intent = Intent(context, MessageReceiveActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        putExtra(EXTRA_EVENT_NAME, EventType.MESSAGE_CLICK.value)
        putExtra(KEY_PUSH_NOTIFICATION_FLAG, "true")
    }

    private fun ignoreIntent(context: Context): Intent = Intent(context, MessageReceiveActivity::class.java).apply {
        action = ACTION_KARTE_IGNORED
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra(EXTRA_EVENT_NAME, EventType.MESSAGE_IGNORE.value)
        putExtra(KEY_PUSH_NOTIFICATION_FLAG, "true")
    }

    private fun Context.componentNameFor(targetActivity: String): String = "$packageName/$targetActivity"

    private companion object {
        private const val DEEPLINK_URI = "karte-test://notifications/click"
        private const val UNRESOLVABLE_DEEPLINK_URI = "karte-test://notifications/unknown"

        private val TARGET_ACTIVITY = NotificationTestTargetActivity::class.java.name
        private const val UNRESOLVABLE_TARGET_ACTIVITY =
            "io.karte.android.notifications.NonExistentNotificationTargetActivity"

        private const val EXTRA_EVENT_NAME = "krt_event_name"
        private const val EXTRA_COMPONENT_NAME = "krt_component_name"
    }
}
