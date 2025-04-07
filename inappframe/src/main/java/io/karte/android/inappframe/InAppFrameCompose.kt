package io.karte.android.inappframe
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.karte.android.inappframe.model.CarouselWithMarginV1
import io.karte.android.inappframe.model.CarouselWithoutMarginV1
import io.karte.android.inappframe.model.CarouselWithoutPagingV1
import io.karte.android.inappframe.model.Empty
import io.karte.android.inappframe.model.IAFTracker
import io.karte.android.inappframe.model.InAppFrameData
import io.karte.android.inappframe.model.InAppFrameDeserializer
import io.karte.android.inappframe.model.SimpleBannerV1
import io.karte.android.variables.Variables
import android.util.Log
import androidx.compose.foundation.layout.Row
import io.karte.android.inappframe.composable.CarouselWithMarginCompose
import io.karte.android.inappframe.composable.CarouselWithoutMarginCompose
import io.karte.android.inappframe.composable.CarouselWithoutPagingCompose
import io.karte.android.inappframe.composable.SimpleBannerCompose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * InAppFrameのCompose版コンポーネント
 *
 * @param placeId 表示場所ID
 * @param modifier Composeのmodifier
 */
@Composable
fun InAppFrameCompose(
    placeId: String,
    modifier: Modifier = Modifier
) {
    var inAppFrameData by remember { mutableStateOf<InAppFrameData?>(null) }
    var iafTracker by remember { mutableStateOf<IAFTracker?>(null) }

    LaunchedEffect(placeId) {
        try {
            val variable = Variables.get("$PREFIX$placeId")
            withContext(Dispatchers.IO) {
                try {
                    val (data, tracker) = InAppFrameDeserializer.deserialize(variable)
                    inAppFrameData = data
                    iafTracker = tracker
                } catch (e: Exception) {
                    Log.d(IN_APP_FRAME_ROOT_LOG, "render failed $e")
                }
            }
        } catch (e: Exception) {
            Log.d(IN_APP_FRAME_ROOT_LOG, "init failed")
        }
    }

    if (inAppFrameData != null && iafTracker != null) {
        InAppFrameContent(inAppFrameData!!, iafTracker!!, modifier)
    }
}

/**
 * Deserializeなどを事前にcoroutineで行い、Composeのコンポーネントを返す関数
 *
 * @param placeId 表示場所ID
 */
/* ktlint-disable */
suspend fun InAppFrame.Companion.loadComposeContent(placeId: String): (@Composable (Modifier) -> Unit)? {
/* ktlint-enable */
    val variable = Variables.get("$PREFIX$placeId")
    try {
        val (inAppFrameData, iafTracker) = withContext(Dispatchers.IO) {
            InAppFrameDeserializer.deserialize(variable)
        }
        return { modifier: Modifier ->
            InAppFrameContent(inAppFrameData, iafTracker, modifier)
        }
    } catch (e: Exception) {
        return null
    }
}

@Composable
private fun InAppFrameContent(
    inAppFrameData: InAppFrameData,
    iafTracker: IAFTracker,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val onBannerClick = { url: String ->
        // URLをパース
        val uri = android.net.Uri.parse(url)
        // InAppFrameからURLを処理すべきか確認
        if (InAppFrame.shouldHandleUrl(uri)) {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        }
    }

    Row(modifier) {
        when (inAppFrameData) {
            is SimpleBannerV1 -> SimpleBannerCompose(
                simpleBanner = inAppFrameData,
                tracker = iafTracker,
                onBannerClick = onBannerClick
            )
            is CarouselWithMarginV1 -> CarouselWithMarginCompose(
                carouselWithMargin = inAppFrameData,
                tracker = iafTracker,
                onBannerClick = onBannerClick
            )
            is CarouselWithoutMarginV1 -> CarouselWithoutMarginCompose(
                carouselWithoutMargin = inAppFrameData,
                tracker = iafTracker,
                onBannerClick = onBannerClick
            )
            is CarouselWithoutPagingV1 -> CarouselWithoutPagingCompose(
                carouselWithoutPaging = inAppFrameData,
                tracker = iafTracker,
                onBannerClick = onBannerClick
            )
            is Empty -> {
                // Empty component, nothing to render
            }
        }
    }
}
