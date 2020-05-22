package io.karte.android.inappmessaging.integration

import android.app.Activity
import android.widget.PopupWindow
import com.google.common.truth.Truth.assertThat
import io.karte.android.inappmessaging.InAppMessaging
import io.karte.android.integration.DryRunTestCase
import org.junit.Test
import org.robolectric.Robolectric

class DryRunTest : DryRunTestCase() {
    @Test
    fun testInAppMessaging() {
        assertThat(InAppMessaging.delegate).isNull()
        assertThat(InAppMessaging.isPresenting).isFalse()

        InAppMessaging.dismiss()
        InAppMessaging.suppress()
        InAppMessaging.unsuppress()

        InAppMessaging.registerWindow(Robolectric.buildActivity(Activity::class.java).get().window)
        InAppMessaging.registerPopupWindow(PopupWindow())

        assertDryRun()
    }
}
