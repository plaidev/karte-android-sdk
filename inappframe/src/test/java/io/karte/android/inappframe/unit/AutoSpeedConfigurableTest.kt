package io.karte.android.inappframe.unit

import io.karte.android.inappframe.components.shared.AutoSpeedConfigurable
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Test

class AutoSpeedConfigurableTest {

    private val autoSpeedConfigurable = object : AutoSpeedConfigurable {}

    @Test
    fun `autoPlaySpeed が null の場合は null を返す`() {
        val result = autoSpeedConfigurable.getTransitionStopMillis(null)
        assertNull(result)
    }

    @Test
    fun `autoPlaySpeed が正の値の場合は (autoPlaySpeed❎1000) を返す`() {
        val result = autoSpeedConfigurable.getTransitionStopMillis(1.5)
        // 1.5 * 1000 = 1500
        assertEquals(1500.toLong(), result)
    }

    @Test
    fun `autoPlaySpeed が 0 の場合は 0 を返す`() {
        val result = autoSpeedConfigurable.getTransitionStopMillis(0.0)
        // 0.0 * 1000 = 0
        assertEquals(0.toLong(), result)
    }

    @Test
    fun `autoPlaySpeed が負の値の場合は負の値を返す`() {
        val result = autoSpeedConfigurable.getTransitionStopMillis(-2.0)
        // -2.0 * 1000 = -2000
        assertEquals(-2000.toLong(), result)
    }
}
