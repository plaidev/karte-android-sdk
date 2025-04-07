package io.karte.android.inappframe.unit

import android.net.Uri
import io.karte.android.inappframe.InAppFrameDelegate
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InAppFrameDelegateTest {

    @Test
    fun testDefaultImplementation_shouldReturnTrue() {
        // デフォルト実装は常にtrueを返す
        val delegate = object : InAppFrameDelegate() {}
        val uri = Uri.parse("https://example.com")
        assertTrue(delegate.shouldOpenURL(uri))
    }
}
