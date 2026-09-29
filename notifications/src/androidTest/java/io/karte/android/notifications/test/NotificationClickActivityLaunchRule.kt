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

package io.karte.android.notifications.test

import android.app.Activity
import android.app.Instrumentation
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import io.karte.android.notifications.MessageReceiveActivity
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import org.junit.rules.ExternalResource

class NotificationClickActivityLaunchRule : ExternalResource() {

    private val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()

    private var messageReceiveMonitor: Instrumentation.ActivityMonitor? = null
    private var targetMonitor: Instrumentation.ActivityMonitor? = null
    private var appLauncherMonitor: Instrumentation.ActivityMonitor? = null
    private val ownedActivities = LinkedHashSet<Activity>()

    override fun before() {
        ownedActivities.clear()
        messageReceiveMonitor = registerMonitor(MessageReceiveActivity::class.java)
        targetMonitor = registerMonitor(NotificationTestTargetActivity::class.java)
        appLauncherMonitor = registerMonitor(NotificationTestLauncherActivity::class.java)
    }

    override fun after() {
        ownedActivities.forEach { it.finish() }
        ownedActivities.clear()
        messageReceiveMonitor?.let(instrumentation::removeMonitor)
        messageReceiveMonitor = null
        targetMonitor?.let(instrumentation::removeMonitor)
        targetMonitor = null
        appLauncherMonitor?.let(instrumentation::removeMonitor)
        appLauncherMonitor = null
    }

    private fun registerMonitor(activityClass: Class<out Activity>): Instrumentation.ActivityMonitor =
        Instrumentation.ActivityMonitor(activityClass.name, null, false).also(instrumentation::addMonitor)

    fun assertOnlyTargetLaunched() {
        assertOnlyLaunched(
            expectedMonitor = checkNotNull(targetMonitor),
            unexpectedMonitor = checkNotNull(appLauncherMonitor),
            expectedClass = NotificationTestTargetActivity::class.java,
            unexpectedClass = NotificationTestLauncherActivity::class.java
        )
    }

    fun assertOnlyAppLauncherLaunched() {
        assertOnlyLaunched(
            expectedMonitor = checkNotNull(appLauncherMonitor),
            unexpectedMonitor = checkNotNull(targetMonitor),
            expectedClass = NotificationTestLauncherActivity::class.java,
            unexpectedClass = NotificationTestTargetActivity::class.java
        )
    }

    fun assertMessageReceiveActivityFinished() {
        val launchedMessageReceiveActivity = checkNotNull(messageReceiveMonitor).observe(EXPECTED_ACTIVITY_TIMEOUT)

        assertWithMessage("Expected ${MessageReceiveActivity::class.java.simpleName} to be created")
            .that(launchedMessageReceiveActivity)
            .isNotNull()

        instrumentation.waitForIdleSync()

        assertWithMessage("Expected ${MessageReceiveActivity::class.java.simpleName} to finish")
            .that(launchedMessageReceiveActivity?.isFinishing)
            .isTrue()
    }

    private fun assertOnlyLaunched(
        expectedMonitor: Instrumentation.ActivityMonitor,
        unexpectedMonitor: Instrumentation.ActivityMonitor,
        expectedClass: Class<out Activity>,
        unexpectedClass: Class<out Activity>
    ) {
        val expectedActivity = expectedMonitor.observe(EXPECTED_ACTIVITY_TIMEOUT)

        assertWithMessage("Expected ${expectedClass.simpleName} to be created")
            .that(expectedActivity)
            .isNotNull()
        assertThat(expectedActivity).isInstanceOf(expectedClass)

        instrumentation.waitForIdleSync()

        val unexpectedActivity = unexpectedMonitor.observe(UNEXPECTED_ACTIVITY_TIMEOUT)
        assertWithMessage("Expected ${unexpectedClass.simpleName} not to be created after ${expectedClass.simpleName}")
            .that(unexpectedActivity)
            .isNull()
    }

    fun assertNoKnownActivityCreatedWithin(timeout: Duration = NO_ACTIVITY_CREATED_TIMEOUT) {
        instrumentation.waitForIdleSync()

        assertNoActivityCreatedWithin(checkNotNull(targetMonitor), timeout)
        assertNoActivityCreatedWithin(checkNotNull(appLauncherMonitor), timeout)
    }

    private fun assertNoActivityCreatedWithin(monitor: Instrumentation.ActivityMonitor, timeout: Duration) =
        assertThat(monitor.observe(timeout)).isNull()

    private fun Instrumentation.ActivityMonitor.observe(timeout: Duration): Activity? =
        waitForActivityWithTimeout(timeout.inWholeMilliseconds)?.also(ownedActivities::add)

    private companion object {
        private val EXPECTED_ACTIVITY_TIMEOUT = 5.seconds
        private val UNEXPECTED_ACTIVITY_TIMEOUT = 500.milliseconds
        private val NO_ACTIVITY_CREATED_TIMEOUT = 500.milliseconds
    }
}
