package io.karte.android.inappframe.composable

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.karte.android.inappframe.model.IAFTracker
import io.karte.android.inappframe.model.IAFVersion
import io.karte.android.inappframe.model.Image
import io.karte.android.inappframe.model.SimpleBannerConfig
import io.karte.android.inappframe.model.SimpleBannerContent
import io.karte.android.inappframe.model.SimpleBannerV1

/**
 * SimpleBannerのCompose版コンポーネント
 *
 * @param simpleBanner SimpleBannerV1データ
 * @param tracker IAFTracker
 * @param onBannerClick バナークリック時のコールバック
 */
@Composable
internal fun SimpleBannerCompose(
    simpleBanner: SimpleBannerV1,
    tracker: IAFTracker,
    onBannerClick: (String) -> Unit
) {
    val content = simpleBanner.content

    // Track open event
    LaunchedEffect(Unit) {
        tracker.trackOpen()
    }

    Card(
        shape = RoundedCornerShape(content.config.radius.dp),
        modifier = Modifier
            .padding(
                start = content.config.paddingStart.dp,
                end = content.config.paddingEnd.dp,
                top = content.config.paddingTop.dp,
                bottom = content.config.paddingBottom.dp
            )
            .fillMaxWidth()
            // Use aspectRatio to calculate height based on the width of the component
            .aspectRatio(ratio = content.config.ratio / 100f)
            .clickable {
                content.data.getOrNull(0)?.linkUrl?.let { url ->
                    if (url.isNotEmpty()) {
                        tracker.trackClick(0, url)
                        onBannerClick(url)
                    }
                }
            }
    ) {
        content.data.getOrNull(0)?.image?.let { image ->
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * プレビュー用のデータを提供するオブジェクト
 * 実際のアプリでは使用しない
 */
private object PreviewData {
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
        templateType = "simpleBanner",
        timestamp = null,
        eventHash = null
    )

    // Create a mock SimpleBannerV1 for preview
    val mockSimpleBanner = SimpleBannerV1(
        componentType = "simpleBanner",
        version = IAFVersion.V1,
        content = SimpleBannerContent(
            data = listOf(
                Image(
                    linkUrl = "https://example.com",
                    image = createMockBitmap(300, 150, Color.BLUE),
                    index = 0
                )
            ),
            config = SimpleBannerConfig(
                paddingStart = 16,
                paddingEnd = 16,
                paddingTop = 8,
                paddingBottom = 8,
                radius = 8,
                ratio = 200
            )
        )
    )
}

/**
 * SimpleBannerComposeのプレビュー
 * 実際のアプリでは使用しない
 */
@Preview(showBackground = true)
@Composable
private fun SimpleBannerComposePreview() {
    SimpleBannerCompose(
        simpleBanner = PreviewData.mockSimpleBanner,
        tracker = PreviewData.mockTracker,
        onBannerClick = { url ->
            // プレビューでは実際のアクションは実行せず、ログ出力のみ
            println("Banner clicked with URL: $url")
        }
    )
}
