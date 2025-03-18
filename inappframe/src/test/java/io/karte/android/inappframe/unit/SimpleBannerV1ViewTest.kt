package io.karte.android.inappframe.unit

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.ImageView
import androidx.test.core.app.ApplicationProvider
import io.karte.android.inappframe.components.SimpleBannerView
import io.karte.android.inappframe.model.IAFTracker
import io.karte.android.inappframe.model.IAFVersion
import io.karte.android.inappframe.model.Image
import io.karte.android.inappframe.model.SimpleBannerV1
import io.karte.android.inappframe.model.SimpleBannerContent
import io.karte.android.inappframe.model.SimpleBannerConfig
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class SimpleBannerV1ViewTest {

    private lateinit var context: Context
    private lateinit var mockTracker: IAFTracker

    @Before
    fun setup() {
        // Robolectric で利用するテスト用のアプリケーション Context を取得
        context = ApplicationProvider.getApplicationContext<Application>()
        // Create mock tracker
        mockTracker = mockk(relaxed = true)
        every { mockTracker.trackOpen() } returns Unit
        every { mockTracker.trackClick(any(), any()) } returns Unit
    }

    @Test
    @Config(qualifiers = "xhdpi")
    fun constructSimpleBannerView_withValidContent_shouldApplyLayoutCorrectly() {
        // テスト用のコンフィグ
        val config = SimpleBannerConfig(
            ratio = 320,
            radius = 8,
            paddingStart = 16,
            paddingEnd = 16,
            paddingTop = 8,
            paddingBottom = 8
        )
        // テスト用のデータ（画像は null で代用）
        val data = listOf(
            Image("https://example.com", null, 0)
        )
        // テスト用のコンテンツ
        val content = SimpleBannerContent(
            config = config,
            data = data
        )
        // SimpleBanner インスタンス生成
        val simpleBanner = SimpleBannerV1("", IAFVersion.V1, content)

        // SimpleBannerView の生成
        val bannerView = SimpleBannerView(context, simpleBanner, mockTracker)

        // Initialize view for testing
        bannerView.setupViewForTesting()

        // レイアウトパラメータやパディングの検証
        // ここでは任意に「ちゃんと適用されているか」を簡易チェックしています
        val paddingLeft = bannerView.paddingLeft
        val paddingRight = bannerView.paddingRight
        val paddingTop = bannerView.paddingTop
        val paddingBottom = bannerView.paddingBottom

        assertTrue("Padding start should be around 16dp in px", paddingLeft == (16 * context.resources.displayMetrics.density).toInt())
        assertTrue("Padding end should be around 16dp in px", paddingRight == (16 * context.resources.displayMetrics.density).toInt())
        assertTrue("Padding top should be around 8dp in px", paddingTop == (8 * context.resources.displayMetrics.density).toInt())
        assertTrue("Padding bottom should be around 8dp in px", paddingBottom == (8 * context.resources.displayMetrics.density).toInt())

        // CardView が子ビューとして存在するか
        assertEquals(1, bannerView.childCount)
        val cardView = bannerView.getChildAt(0)

        // CardView に ImageView が存在するか
        val imageViewContainer = (cardView as? androidx.cardview.widget.CardView)
        assertNotNull("CardView should not be null", imageViewContainer)
        assertEquals("CardView should have exactly 1 child", 1, imageViewContainer?.childCount)

        // CardViewの高さが正しいか
        val cardViewHeight = imageViewContainer?.layoutParams?.height
        val cardViewWidth = imageViewContainer?.layoutParams?.width

        // 画面の幅を取得
        val displayWidth = context.resources.displayMetrics.widthPixels
        // パディングを考慮した実際の表示可能幅を計算
        val availableWidth = displayWidth - paddingLeft - paddingRight
        // 比率から高さを計算（ratio は width:height = ratio:100 の比率）
        val expectedHeight = (availableWidth * 100) / config.ratio
        assertEquals("CardView height should be calculated based on the ratio", expectedHeight, cardViewHeight)
        assertEquals("CardView width should be calculated based on the ratio", availableWidth, cardViewWidth)

        val imageView = imageViewContainer?.getChildAt(0) as? ImageView
        assertNotNull("Child of CardView should be an ImageView", imageView)

        assertEquals(ImageView.ScaleType.CENTER_CROP, imageView?.scaleType)
    }

    @Test
    fun imageIsNull_shouldSetTransparentDrawable() {
        val config = SimpleBannerConfig(
            ratio = 320,
            radius = 8,
            paddingStart = 16,
            paddingEnd = 16,
            paddingTop = 8,
            paddingBottom = 8
        )
        // 画像が null のテスト用データ
        val data = listOf(
            Image("https://example.com", null, 0)
        )
        val content = SimpleBannerContent(data, config)
        val simpleBanner = SimpleBannerV1("", IAFVersion.V1, content)

        val bannerView = SimpleBannerView(context, simpleBanner, mockTracker)

        // Initialize view for testing
        bannerView.setupViewForTesting()

        val cardView = bannerView.getChildAt(0) as androidx.cardview.widget.CardView
        val imageView = cardView.getChildAt(0) as ImageView

        // setImageBitmap(image) の代わりに setImageResource(android.R.color.transparent)
        // がコールされているはずなので Drawable が null ではないかを確認
        // Robolectric では ResourceId が引き当てられるため、実際には non-null になる想定
        assertNotNull(imageView.drawable)
    }

    @Test
    fun clickOnBanner_shouldStartBrowserIntent() {
        val config = SimpleBannerConfig(
            ratio = 320,
            radius = 8,
            paddingStart = 16,
            paddingEnd = 16,
            paddingTop = 8,
            paddingBottom = 8
        )
        val data = listOf(
            Image("https://example.com", null, 0)
        )
        val content = SimpleBannerContent(
            config = config,
            data = data
        )
        val simpleBanner = SimpleBannerV1("", IAFVersion.V1, content)

        // Activityのインスタンスを作成
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val bannerView = SimpleBannerView(activity, simpleBanner, mockTracker)

        // Initialize view for testing
        bannerView.setupViewForTesting()

        // CardView を取得
        val cardView = bannerView.getChildAt(0)
        // クリックイベント発火
        cardView.performClick()

        // Robolectric の ShadowApplication を使って Intent を取得
        val shadowActivity = Shadows.shadowOf(activity)
        val startedIntent = shadowActivity.nextStartedActivity

        // 開いた Intent が正しいかを検証
        assertNotNull("Intent should be started", startedIntent)
        assertEquals(Intent.ACTION_VIEW, startedIntent.action)
        assertEquals(Uri.parse("https://example.com"), startedIntent.data)
    }

    @Test
    fun imageIsNotNull_shouldSetImageBitmap() {
        val config = SimpleBannerConfig(
            ratio = 320,
            radius = 8,
            paddingStart = 16,
            paddingEnd = 16,
            paddingTop = 8,
            paddingBottom = 8
        )
        // ダミーの Bitmap を用意
        val dummyBitmap = android.graphics.Bitmap.createBitmap(100, 50, android.graphics.Bitmap.Config.ARGB_8888)
        val data = listOf(Image("", dummyBitmap, 0))
        val content = SimpleBannerContent(
            config = config,
            data = data
        )
        val simpleBanner = SimpleBannerV1("", IAFVersion.V1, content)

        val bannerView = SimpleBannerView(context, simpleBanner, mockTracker)

        // Initialize view for testing
        bannerView.setupViewForTesting()

        val cardView = bannerView.getChildAt(0) as androidx.cardview.widget.CardView
        val imageView = cardView.getChildAt(0) as ImageView

        // ImageView にセットされている Bitmap を確認
        val drawable = imageView.drawable
        assertNotNull(drawable)
        assertTrue(drawable is android.graphics.drawable.BitmapDrawable)
        assertEquals((drawable as android.graphics.drawable.BitmapDrawable).bitmap, dummyBitmap)
    }
}
