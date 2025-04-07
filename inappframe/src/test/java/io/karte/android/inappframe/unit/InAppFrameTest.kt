package io.karte.android.inappframe.unit

import android.net.Uri
import io.karte.android.inappframe.InAppFrame
import io.karte.android.inappframe.InAppFrameDelegate
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InAppFrameTest {

    @After
    fun tearDown() {
        // テスト後にリスナーとデリゲートをクリア
        InAppFrame.setDelegate(null)
    }

    @Test
    fun testDelegate_whenDelegateAllowsUrl_shouldReturnTrue() {
        // デリゲートを設定 - すべてのURLを処理する
        InAppFrame.setDelegate(object : InAppFrameDelegate() {
            override fun shouldOpenURL(url: Uri): Boolean {
                return true
            }
        })

        val uri = Uri.parse("https://example.com/path")
        assertTrue(InAppFrame.shouldHandleUrl(uri))
    }

    @Test
    fun testDelegate_whenDelegateBlocksUrl_shouldReturnFalse() {
        // デリゲートを設定 - 特定のホストのURLを処理しない
        InAppFrame.setDelegate(object : InAppFrameDelegate() {
            override fun shouldOpenURL(url: Uri): Boolean {
                return url.host != "blocked-domain.example.com"
            }
        })

        // 処理されるべきURL
        val allowedUri = Uri.parse("https://example.com/path")
        assertTrue(InAppFrame.shouldHandleUrl(allowedUri))

        // 処理されないべきURL
        val blockedUri = Uri.parse("https://blocked-domain.example.com/path")
        assertFalse(InAppFrame.shouldHandleUrl(blockedUri))
    }
}
