package io.karte.android.inappframe.composable

import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import io.karte.android.inappframe.model.IAFTracker

/**
 * URLの有無に基づいてクリック可能なModifierを適用する拡張関数
 *
 * @param url クリック時に開くURL（空の場合はクリック不可）
 * @param tracker クリックイベントを追跡するIAFTracker
 * @param index トラッキング用のインデックス
 * @param onBannerClick バナークリック時のコールバック
 * @param onInteraction ユーザーインタラクション時に呼び出されるオプションのコールバック
 * @return URLが存在する場合はクリック可能なModifier、存在しない場合は元のModifier
 */
internal fun Modifier.conditionalClickable(
    url: String?,
    tracker: IAFTracker,
    index: Int,
    onBannerClick: (String) -> Unit,
    onInteraction: (() -> Unit)? = null
): Modifier {
    return if (!url.isNullOrEmpty()) {
        this.clickable {
            onInteraction?.invoke()
            tracker.trackClick(index, url)
            onBannerClick(url)
        }
    } else {
        this
    }
}
