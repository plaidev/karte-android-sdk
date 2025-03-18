package io.karte.android.inappframe.composable

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import io.karte.android.inappframe.model.CarouselWithMarginConfig
import io.karte.android.inappframe.model.CarouselWithMarginContent
import io.karte.android.inappframe.model.CarouselWithMarginV1
import io.karte.android.inappframe.model.IAFTracker
import io.karte.android.inappframe.model.IAFVersion
import io.karte.android.inappframe.model.Image
import kotlinx.coroutines.delay

/**
 * 表示領域よりもbannerの横幅が大きいと、pagerがクラッシュしてしまうので、調整をするための関数
 *
 * @param density Compose Density for unit conversion
 * @param componentWidthPx Component width in pixels
 * @param initialItemWidth Initial item width in dp
 * @param bannerHeight Banner height in pixels
 * @param ratio Aspect ratio (width/height)
 * @return Pair of adjusted width and height in dp
 */
internal fun calculateAdjustedDimensions(
    density: Density,
    componentWidthPx: Int,
    initialItemWidth: androidx.compose.ui.unit.Dp,
    bannerHeight: Int,
    ratio: Float
): Pair<androidx.compose.ui.unit.Dp, androidx.compose.ui.unit.Dp> {
    // Default values
    var itemWidth = initialItemWidth
    var itemHeight = bannerHeight.dp

    if (componentWidthPx > 0) {
        // Convert initialItemWidth to pixels for comparison
        val itemWidthPx = with(density) { initialItemWidth.toPx() }

        // Calculate horizontal padding in pixels
        val horizontalPaddingPx = (componentWidthPx - itemWidthPx) / 2

        // Check if the item width plus padding exceeds the component width
        if (horizontalPaddingPx < 0) {
            // Adjust the item width to fit within the component width with some padding
            val availableWidthPx = componentWidthPx * 0.9f // 90% of component width to leave some padding

            // Convert back to dp for use in the UI
            itemWidth = with(density) { availableWidthPx.toDp() }

            // Recalculate height to maintain aspect ratio
            itemHeight = with(density) { (availableWidthPx / ratio).toDp() }
        }
    }

    return Pair(itemWidth, itemHeight)
}

/**
 * CarouselWithMarginのCompose版コンポーネント
 *
 * @param carouselWithMargin CarouselWithMarginV1データ
 * @param tracker IAFTracker
 * @param onBannerClick バナークリック時のコールバック
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CarouselWithMarginCompose(
    carouselWithMargin: CarouselWithMarginV1,
    tracker: IAFTracker,
    onBannerClick: (String) -> Unit
) {
    val content = carouselWithMargin.content
    val pagerState = rememberPagerState { content.data.size }
    var userInteracted by remember { mutableStateOf(false) }

    // Calculate the initial item width based on ratio
    val initialItemWidth = (content.config.bannerHeight * (content.config.ratio / 100f)).toInt().dp

    // Only keep componentWidthPx as state
    var componentWidthPx by remember { mutableIntStateOf(0) }

    // Track open event
    LaunchedEffect(Unit) {
        tracker.trackOpen()
    }

    // Auto-scroll if autoplaySpeed is set
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

    Box(
        modifier = Modifier
            .padding(
                top = content.config.paddingTop.dp,
                bottom = content.config.paddingBottom.dp
            )
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                componentWidthPx = coordinates.size.width
            }
    ) {
        if (componentWidthPx != 0) {
            val density = LocalDensity.current
            val ratio = content.config.ratio / 100f
            val (itemWidth, itemHeight) = calculateAdjustedDimensions(
                density = density,
                componentWidthPx = componentWidthPx,
                initialItemWidth = initialItemWidth,
                bannerHeight = content.config.bannerHeight,
                ratio = ratio
            )
            val horizontalPadding = with(density) {
                if (componentWidthPx > 0) {
                    (componentWidthPx.toDp() - itemWidth) / 2
                } else {
                    0.dp
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .height(itemHeight),
                pageSpacing = content.config.spacing.dp,
                pageSize = PageSize.Fixed(itemWidth),
                beyondBoundsPageCount = 5,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = horizontalPadding,
                    end = horizontalPadding
                )
            ) { page ->
                val image = content.data.getOrNull(page)

                if (image != null) {
                    Card(
                        shape = RoundedCornerShape(content.config.radius.dp),
                        modifier = Modifier
                            .width(itemWidth)
                            .height(itemHeight)
                            .clickable {
                                userInteracted = true
                                if (image.linkUrl.isNotEmpty()) {
                                    tracker.trackClick(page, image.linkUrl)
                                    onBannerClick(image.linkUrl)
                                }
                            }
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
        }
    }
}

/**
 * プレビュー用のデータを提供するオブジェクト
 * 実際のアプリでは使用しない
 */
private object CarouselWithMarginPreviewData {
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
        templateType = "carouselWithMargin",
        timestamp = null,
        eventHash = null
    )

    // Create a mock CarouselWithMarginV1 for preview
    val mockCarouselWithMargin = CarouselWithMarginV1(
        componentType = "carouselWithMargin",
        version = IAFVersion.V1,
        content = CarouselWithMarginContent(
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
                ),
                Image(
                    linkUrl = "https://example.com/4",
                    image = createMockBitmap(300, 150, Color.YELLOW),
                    index = 3
                ),
                Image(
                    linkUrl = "https://example.com/5",
                    image = createMockBitmap(300, 150, Color.CYAN),
                    index = 4
                )
            ),
            config = CarouselWithMarginConfig(
                bannerHeight = 150,
                paddingTop = 8,
                paddingBottom = 8,
                radius = 8,
                spacing = 10,
                ratio = 200,
                autoplaySpeed = 3.0,
                templateType = "carouselWithMargin"
            )
        )
    )
}

/**
 * CarouselWithMarginComposeのプレビュー
 * 実際のアプリでは使用しない
 */
@Preview(showBackground = true)
@Composable
private fun CarouselWithMarginComposePreview() {
    CarouselWithMarginCompose(
        carouselWithMargin = CarouselWithMarginPreviewData.mockCarouselWithMargin,
        tracker = CarouselWithMarginPreviewData.mockTracker,
        onBannerClick = { url ->
            // プレビューでは実際のアクションは実行せず、ログ出力のみ
            println("Banner clicked with URL: $url")
        }
    )
}
