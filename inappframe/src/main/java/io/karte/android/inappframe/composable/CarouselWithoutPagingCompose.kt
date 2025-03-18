package io.karte.android.inappframe.composable

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.karte.android.inappframe.model.CarouselWithoutPagingConfig
import io.karte.android.inappframe.model.CarouselWithoutPagingContent
import io.karte.android.inappframe.model.CarouselWithoutPagingV1
import io.karte.android.inappframe.model.IAFTracker
import io.karte.android.inappframe.model.IAFVersion
import io.karte.android.inappframe.model.Image

/**
 * CarouselWithoutPagingのCompose版コンポーネント
 *
 * @param carouselWithoutPaging CarouselWithoutPagingV1データ
 * @param tracker IAFTracker
 * @param onBannerClick バナークリック時のコールバック
 */
@Composable
internal fun CarouselWithoutPagingCompose(
    carouselWithoutPaging: CarouselWithoutPagingV1,
    tracker: IAFTracker,
    onBannerClick: (String) -> Unit
) {
    val content = carouselWithoutPaging.content

    // Track open event
    LaunchedEffect(Unit) {
        tracker.trackOpen()
    }

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = content.config.paddingStart.dp,
            end = content.config.paddingEnd.dp,
            top = content.config.paddingTop.dp,
            bottom = content.config.paddingBottom.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(content.config.spacing.dp)
    ) {
        itemsIndexed(content.data) { index, image ->
            Card(
                shape = RoundedCornerShape(content.config.radius.dp),
                modifier = Modifier
                    .width((content.config.bannerHeight * (content.config.ratio / 100f)).toInt().dp)
                    .height(content.config.bannerHeight.dp)
                    .clickable {
                        if (image.linkUrl.isNotEmpty()) {
                            tracker.trackClick(index, image.linkUrl)
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

/**
 * プレビュー用のデータを提供するオブジェクト
 * 実際のアプリでは使用しない
 */
private object CarouselWithoutPagingPreviewData {
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
        templateType = "carouselWithoutPaging",
        timestamp = null,
        eventHash = null
    )

    // Create a mock CarouselWithoutPagingV1 for preview
    val mockCarouselWithoutPaging = CarouselWithoutPagingV1(
        componentType = "carouselWithoutPaging",
        version = IAFVersion.V1,
        content = CarouselWithoutPagingContent(
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
            config = CarouselWithoutPagingConfig(
                paddingStart = 16,
                paddingEnd = 16,
                paddingTop = 8,
                paddingBottom = 8,
                spacing = 8,
                radius = 8,
                bannerHeight = 150,
                ratio = 200,
                autoplaySpeed = null
            )
        )
    )
}

/**
 * CarouselWithoutPagingComposeのプレビュー
 * 実際のアプリでは使用しない
 */
@Preview(showBackground = true)
@Composable
private fun CarouselWithoutPagingComposePreview() {
    CarouselWithoutPagingCompose(
        carouselWithoutPaging = CarouselWithoutPagingPreviewData.mockCarouselWithoutPaging,
        tracker = CarouselWithoutPagingPreviewData.mockTracker,
        onBannerClick = { url ->
            // プレビューでは実際のアクションは実行せず、ログ出力のみ
            println("Banner clicked with URL: $url")
        }
    )
}
