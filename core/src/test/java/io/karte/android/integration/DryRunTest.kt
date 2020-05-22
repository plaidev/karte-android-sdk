package io.karte.android.integration

import android.webkit.WebView
import com.google.common.truth.Truth.assertThat
import io.karte.android.KarteApp
import io.karte.android.core.config.Config
import io.karte.android.core.logger.LogLevel
import io.karte.android.core.usersync.UserSync
import io.karte.android.getThreadByName
import io.karte.android.setupKarteApp
import io.karte.android.tracking.Event
import io.karte.android.tracking.Tracker
import io.karte.android.tracking.TrackerDelegate
import org.junit.Before
import org.junit.Test

open class DryRunTestCase : SetupTestCase() {
    @Before
    fun setup() {
        setupKarteApp(server, appKey, Config.Builder().isDryRun(true))
    }

    /**Queue用のスレッドが生成されていないか、serverにリクエストが飛んでないか確認.*/
    fun assertDryRun() {
        assertThat(getThreadByName()).isNull()
        assertThat(server.requestCount).isEqualTo(0)
    }
}

class DryRunTest : DryRunTestCase() {
    @Test
    fun testKarteApp() {
        assertThat(KarteApp.isOptOut).isFalse()
        assertThat(KarteApp.visitorId).isEmpty()

        KarteApp.setLogLevel(LogLevel.VERBOSE)
        KarteApp.optIn()
        KarteApp.optOut()
        KarteApp.renewVisitorId()
        assertDryRun()
    }

    @Test
    fun testTracker() {
        Tracker.setDelegate(object : TrackerDelegate {
            override fun intercept(event: Event): Event {
                return event
            }
        })
        Tracker.identify(mapOf("user_id" to "dummy"))
        Tracker.view("test")
        Tracker.track("test")

        assertDryRun()
    }

    @Test
    fun testUserSync() {
        assertThat(UserSync.appendUserSyncQueryParameter("test")).isEqualTo("test")

        UserSync.setUserSyncScript(WebView(application))
        assertDryRun()
    }
}
