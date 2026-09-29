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
class MessageReceiverTest {

    @get:Rule
    val activityLaunchRule = NotificationClickActivityLaunchRule()

    @Test
    fun onReceive_whenDeeplinkTargetCanBeResolved_launchesTargetActivity() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = clickIntentWithDeeplink(DEEPLINK_URI)

        MessageReceiver().onReceive(context, intent)

        activityLaunchRule.assertOnlyTargetLaunched()
    }

    @Test
    fun onReceive_whenDeeplinkTargetCannotBeResolved_launchesAppLauncher() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = clickIntentWithDeeplink(UNRESOLVABLE_DEEPLINK_URI)

        MessageReceiver().onReceive(context, intent)

        activityLaunchRule.assertOnlyAppLauncherLaunched()
    }

    @Test
    fun onReceive_whenTargetActivityCanBeResolved_launchesTargetActivity() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val componentName = context.componentNameFor(TARGET_ACTIVITY)
        val intent = clickIntentWithComponentName(componentName)

        MessageReceiver().onReceive(context, intent)

        activityLaunchRule.assertOnlyTargetLaunched()
    }

    @Test
    fun onReceive_whenTargetActivityCannotBeResolved_launchesAppLauncher() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val componentName = context.componentNameFor(UNRESOLVABLE_TARGET_ACTIVITY)
        val intent = clickIntentWithComponentName(componentName)

        MessageReceiver().onReceive(context, intent)

        activityLaunchRule.assertOnlyAppLauncherLaunched()
    }

    @Test
    fun onReceive_whenIgnoreEvent_doesNotLaunchActivity() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val ignoreIntent = ignoreIntent()

        MessageReceiver().onReceive(context, ignoreIntent)

        activityLaunchRule.assertNoKnownActivityCreatedWithin()
    }

    private fun clickIntentWithComponentName(componentName: String): Intent = clickIntent().apply {
        putExtra(EXTRA_COMPONENT_NAME, componentName)
    }

    @SuppressLint("UseKtx")
    private fun clickIntentWithDeeplink(deeplink: String): Intent = clickIntent().apply {
        action = Intent.ACTION_VIEW
        data = Uri.parse(deeplink)
    }

    private fun clickIntent(): Intent = Intent().apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        putExtra(EXTRA_EVENT_NAME, EventType.MESSAGE_CLICK.value)
        putExtra(KEY_PUSH_NOTIFICATION_FLAG, "true")
    }

    private fun ignoreIntent(): Intent = Intent(ACTION_KARTE_IGNORED).apply {
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
