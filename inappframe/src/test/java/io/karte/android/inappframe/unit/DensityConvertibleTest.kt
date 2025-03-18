package io.karte.android.inappframe.unit

import android.content.Context
import android.content.res.Resources
import android.util.DisplayMetrics
import io.karte.android.inappframe.components.shared.DensityConvertible
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class DensityConvertibleTest {

    private val densityConvertible = object : DensityConvertible {}

    @Test
    fun dpが20でdensityが2の場合() {
        // MockKでContextを作成
        val mockContext = mockk<Context>(relaxed = true)
        val mockResources = mockk<Resources>(relaxed = true)
        val displayMetrics = DisplayMetrics().apply { density = 2.0f }

        // Mockの動作を定義
        every { mockContext.resources } returns mockResources
        every { mockResources.displayMetrics } returns displayMetrics

        val result = densityConvertible.dpToPx(mockContext, 10)

        assertEquals(20, result)
    }

    @Test
    fun dpが0でdensityが2の場合() {
        val mockContext = mockk<Context>(relaxed = true)
        val mockResources = mockk<Resources>(relaxed = true)
        val displayMetrics = DisplayMetrics().apply { density = 2.0f }

        every { mockContext.resources } returns mockResources
        every { mockResources.displayMetrics } returns displayMetrics

        val result = densityConvertible.dpToPx(mockContext, 0)

        assertEquals(0, result)
    }

    @Test
    fun dpが20でdensityが1の場合() {
        val mockContext = mockk<Context>(relaxed = true)
        val mockResources = mockk<Resources>(relaxed = true)
        val displayMetrics = DisplayMetrics().apply { density = 1.0f }

        every { mockContext.resources } returns mockResources
        every { mockResources.displayMetrics } returns displayMetrics

        val result = densityConvertible.dpToPx(mockContext, 20)

        assertEquals(20, result)
    }
}
