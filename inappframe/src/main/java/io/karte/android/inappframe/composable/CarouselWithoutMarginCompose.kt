package io.karte.android.inappframe.composable

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.karte.android.inappframe.model.CarouselWithoutMarginConfig
import io.karte.android.inappframe.model.CarouselWithoutMarginContent
import io.karte.android.inappframe.model.CarouselWithoutMarginV1
import io.karte.android.inappframe.model.IAFTracker
import io.karte.android.inappframe.model.IAFVersion
import io.karte.android.inappframe.model.Image
import kotlinx.coroutines.delay

/**
 * CarouselWithoutMarginのCompose版コンポーネント
 *
 * @param carouselWithoutMargin CarouselWithoutMarginV1データ
 * @param tracker IAFTracker
 * @param onBannerClick バナークリック時のコールバック
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CarouselWithoutMarginCompose(
    carouselWithoutMargin: CarouselWithoutMarginV1,
    tracker: IAFTracker,
    onBannerClick: (String) -> Unit
) {
    val content = carouselWithoutMargin.content
    val pagerState = rememberPagerState { content.data.size }
    var userInteracted by remember { mutableStateOf(false) }

    // Track open event
    LaunchedEffect(Unit) {
        tracker.trackOpen()
    }

    LaunchedEffect(userInteracted) {
        // autoplaySpeedMillis: 自動スクロールの間隔（ミリ秒）として定義（例: 3000ミリ秒なら3秒）
        val autoplaySpeedMillis = (content.config.autoplaySpeed?.times(1000))?.toLong() ?: 0L
        // autoplaySpeedが設定されていない場合は自動スクロール処理を行わない
        if (autoplaySpeedMillis <= 0L) return@LaunchedEffect

        while (true) {
            // ユーザー操作がない場合のみ自動スクロールを行う
            if (!userInteracted) {
                delay(autoplaySpeedMillis)
                // 次のページへ。最後のページの場合は最初のページに戻る
                val nextPage = if (pagerState.currentPage < content.data.size - 1) {
                    pagerState.currentPage + 1
                } else {
                    0
                }
                pagerState.animateScrollToPage(nextPage)
            } else {
                // ユーザー操作があった場合は、短い間隔で再度チェック（または必要に応じてbreakで停止）
                break
            }
        }
    }
    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .padding(
                top = content.config.paddingTop.dp,
                bottom = content.config.paddingBottom.dp
            )
            .fillMaxWidth(),
        beyondViewportPageCount = 2
    ) { page ->
        val image = content.data.getOrNull(page) ?: return@HorizontalPager
        Card(
            shape = RoundedCornerShape(content.config.radius.dp),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(content.config.ratio / 100f)
                .conditionalClickable(
                    url = image.linkUrl,
                    tracker = tracker,
                    index = page,
                    onBannerClick = onBannerClick,
                    onInteraction = { userInteracted = true }
                )
        ) {
            image.image?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * プレビュー用のデータを提供するオブジェクト
 * 実際のアプリでは使用しない
 */
private object CarouselWithoutMarginPreviewData {
    // Create a mock bitmap for preview
    private fun createMockBitmap(width: Int, height: Int, color: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(color)
        return bitmap
    }

    // Create a mock IAFTracker for preview
    val mockTracker = IAFTracker(
        campaignId = "preview-campaign",
        shortenId = "preview-shorten",
        templateType = "carouselWithoutMargin",
        timestamp = null,
        eventHash = null
    )

    // Create a mock CarouselWithoutMarginV1 for preview
    val mockCarouselWithoutMargin = CarouselWithoutMarginV1(
        componentType = "carouselWithoutMargin",
        version = IAFVersion.V1,
        content = CarouselWithoutMarginContent(
            data = listOf(
                Image(
                    linkUrl = "https://example.com/1",
                    image = createMockBitmap(300, 150, Color.BLUE),
                    index = 0
                ),
                Image(
                    linkUrl = "https://example.com/2",
                    image = createMockBitmap(300, 150, Color.RED),
                    index = 1
                ),
                Image(
                    linkUrl = "https://example.com/3",
                    image = createMockBitmap(300, 150, Color.GREEN),
                    index = 2
                )
            ),
            config = CarouselWithoutMarginConfig(
                paddingTop = 8,
                paddingBottom = 8,
                radius = 8,
                ratio = 200,
                autoplaySpeed = 3.0,
                templateType = "carouselWithoutMargin"
            )
        )
    )
}

/**
 * CarouselWithoutMarginComposeのプレビュー
 * 実際のアプリでは使用しない
 */
@Preview(showBackground = true)
@Composable
private fun CarouselWithoutMarginComposePreview() {
    CarouselWithoutMarginCompose(
        carouselWithoutMargin = CarouselWithoutMarginPreviewData.mockCarouselWithoutMargin,
        tracker = CarouselWithoutMarginPreviewData.mockTracker,
        onBannerClick = { url ->
            // プレビューでは実際のアクションは実行せず、ログ出力のみ
            println("Banner clicked with URL: $url")
        }
    )
}
