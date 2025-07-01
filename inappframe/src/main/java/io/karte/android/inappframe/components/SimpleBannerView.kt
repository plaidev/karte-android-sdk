package io.karte.android.inappframe.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat.startActivity
import io.karte.android.inappframe.InAppFrame
import io.karte.android.inappframe.components.shared.DensityConvertible
import io.karte.android.inappframe.model.IAFTracker
import io.karte.android.inappframe.model.SimpleBannerV1
import io.karte.android.inappframe.model.SimpleBannerContent

internal class SimpleBannerView private constructor(context: Context) : LinearLayout(context), DensityConvertible {

    // Keep a reference to content & tracker so we can access them later
    private lateinit var content: SimpleBannerContent
    private lateinit var tracker: IAFTracker

    internal constructor(context: Context, simpleBanner: SimpleBannerV1, tracker: IAFTracker) : this(context) {
        this.content = simpleBanner.content
        this.tracker = tracker

        // Then compute available width by subtracting padding.
        val paddingStartPx = dpToPx(context, content.config.paddingStart)
        val paddingEndPx = dpToPx(context, content.config.paddingEnd)
        val paddingTopPx = dpToPx(context, content.config.paddingTop)
        val paddingBottomPx = dpToPx(context, content.config.paddingBottom)
        setPadding(paddingStartPx, paddingTopPx, paddingEndPx, paddingBottomPx)

        layoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Wait for layout to be done before measuring
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                setupViewAfterLayout()
            }
        })
    }

    /**
     * Called once this view and its parent have been laid out, so measuredWidth is valid.
     */
    private fun setupViewAfterLayout() {
        val parentWidth = width
        val availableWidth = parentWidth - dpToPx(context, content.config.paddingStart) - dpToPx(context, content.config.paddingEnd)

        // ratio means width:height = ratio:100
        val height = (availableWidth * 100) / content.config.ratio

        val cardView = CardView(context).apply {
            radius = dpToPx(context, content.config.radius).toFloat()
            cardElevation = 0f
            layoutParams = LayoutParams(availableWidth, height)
            setOnClickListener {
                content.data.getOrNull(0)?.linkUrl?.let { url ->
                    if (url.isEmpty()) return@setOnClickListener
                    tracker.trackClick(0, url)
                    val uri = Uri.parse(url)
                    // InAppFrameからURLを処理すべきか確認
                    if (InAppFrame.shouldHandleUrl(uri)) {
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        startActivity(context, intent, null)
                    }
                }
            }
        }

        val imageView = createImageView(content)
        cardView.addView(imageView)

        // Clear any existing views first if you re-run setup
        removeAllViews()
        addView(cardView)
        tracker.trackOpen()
    }

    private fun createImageView(simpleBannerContent: SimpleBannerContent): ImageView {
        return ImageView(context).apply {
            simpleBannerContent.data.getOrNull(0)?.image?.let { image ->
                setImageBitmap(image)
            } ?: run {
                setImageResource(android.R.color.transparent)
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }
}
