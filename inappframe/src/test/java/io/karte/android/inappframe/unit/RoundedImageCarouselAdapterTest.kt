package io.karte.android.inappframe.unit

import android.content.Context
import android.graphics.Bitmap
import android.widget.ImageView
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import io.karte.android.inappframe.model.Image
import io.karte.android.inappframe.components.shared.RoundedImageCarouselAdapter
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import android.content.res.Resources
import android.graphics.Outline
import android.view.ViewOutlineProvider
import androidx.core.graphics.drawable.RoundedBitmapDrawable

class RoundedImageCarouselAdapterTest {

    @MockK
    lateinit var context: Context

    @MockK
    lateinit var resources: Resources

    @MockK
    lateinit var imageView: ImageView

    private fun createMockBitmap(): Bitmap {
        val mockBitmap = mockk<Bitmap>(relaxed = true)
        every { mockBitmap.width } returns 10
        every { mockBitmap.height } returns 10
        return mockBitmap
    }

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        clearAllMocks()

        // static メソッドをモック化
        mockkStatic(RoundedBitmapDrawableFactory::class)

        // Context.resources などの戻り値設定
        every { context.resources } returns resources

        // ImageView に対しては特に何も返さないが、呼び出しは無害に処理される
        every { imageView.setImageDrawable(any()) } returns Unit
        every { imageView.setOutlineProvider(any()) } returns Unit
        every { imageView.clipToOutline = any() } returns Unit
        every { imageView.scaleType = any() } returns Unit
        every { imageView.width } returns 100
        every { imageView.height } returns 100
    }

    @Test
    fun `count() は imageList のサイズを返す`() {
        val images = listOf(
            Image(index = 0, image = createMockBitmap(), linkUrl = "image1"),
            Image(index = 1, image = createMockBitmap(), linkUrl = "image2"),
            Image(index = 2, image = createMockBitmap(), linkUrl = "image3")
        )
        val adapter = RoundedImageCarouselAdapter(
            context = context,
            resources = resources,
            radius = 8,
            imageList = images
        )

        val count = adapter.count()
        assertEquals(3, count)
    }

    @Test
    fun `populate() で ImageView に丸角画像がセットされるか検証`() {
        val images = listOf(
            Image(index = 0, image = createMockBitmap(), linkUrl = "image1")
        )

        // Adapter作成
        val adapter = object : RoundedImageCarouselAdapter(
            context = context,
            resources = resources,
            radius = 16,
            imageList = images
        ) {
            // density=2.0f 相当の計算を行う
            override fun dpToPx(context: Context, dp: Int): Int {
                return dp * 2
            }
        }

        val mockDrawable = mockk<RoundedBitmapDrawable>(relaxed = true)
        every { RoundedBitmapDrawableFactory.create(resources, images[0].image) } returns mockDrawable

        // テスト対象メソッド呼び出し
        adapter.populate(imageView, index = 0)

        // (1) outlineProvider が設定されている
        val outlineProviderSlot = slot<ViewOutlineProvider>()
        verify(exactly = 1) {
            imageView.outlineProvider = capture(outlineProviderSlot)
        }

        // 実際に outlineProvider.getOutline(...) が呼ばれた場合の動作を簡易検証
        val outline = mockk<Outline>(relaxed = true)
        outlineProviderSlot.captured.getOutline(imageView, outline)

        // (2) outLinProvider.getOutline()でgetWidth()とgetHeight()が呼ばれ正しい引数でsetRoundRect()が呼ばれている
        verify { imageView.width }
        verify { imageView.height }
        verify { outline.setRoundRect(0, 0, 100, 100, 32.0f) }

        // (3) clipToOutline = true
        verify { imageView.clipToOutline = true }

        // (4) scaleType = CENTER_CROP
        verify { imageView.scaleType = ImageView.ScaleType.CENTER_CROP }

        // (5) RoundedBitmapDrawableFactory.create(...) が呼ばれ
        //     cornerRadius が設定され、 setImageDrawable(...) されている
        verify {
            RoundedBitmapDrawableFactory.create(resources, images[0].image)
            mockDrawable.cornerRadius = 32.0f // radius=16, dpToPx=16*2=32 → float 32.0
            imageView.setImageDrawable(mockDrawable)
        }

        confirmVerified(imageView, mockDrawable)
    }

    @Test
    fun `onNewItem(index) - 基本的には何もしない`() {
        val adapter = RoundedImageCarouselAdapter(
            context = context,
            resources = resources,
            radius = 8,
            imageList = emptyList()
        )

        adapter.onNewItem(0)

        confirmVerified(imageView)
    }
}
