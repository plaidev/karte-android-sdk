package io.karte.android.inappframe.components

import android.content.Context
import android.content.Intent
import android.graphics.Outline
import android.net.Uri
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat.startActivity
import io.karte.android.inappframe.model.CarouselWithoutPagingV1
import io.karte.android.inappframe.model.IAFTracker
import io.karte.android.inappframe.components.shared.DensityConvertible

internal class CarouselWithoutPagingView private constructor(context: Context) : LinearLayout(context), DensityConvertible {
    internal constructor(context: Context, carouselWithoutPaging: CarouselWithoutPagingV1, iafTracker: IAFTracker) : this(context) {
        val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        // Set the modified layout parameters back to the view
        this.layoutParams = params
        carouselWithoutPaging.content.config.apply {
            val paddingStartPx = dpToPx(context, paddingStart)
            val paddingTopPx = dpToPx(context, paddingTop)
            val paddingEndPx = dpToPx(context, paddingEnd)
            val paddingBottomPx = dpToPx(context, paddingBottom)
            setPadding(paddingStartPx, paddingTopPx, paddingEndPx, paddingBottomPx)
        }

        addHorizontalScrollView(carouselWithoutPaging, iafTracker)
    }

    private fun addHorizontalScrollView(carouselWithoutPaging: CarouselWithoutPagingV1, iafTracker: IAFTracker) {
        // Create a HorizontalScrollView
        val horizontalScrollView = HorizontalScrollView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            isHorizontalScrollBarEnabled = false
        }

        // Create a LinearLayout to hold ImageViews
        val innerLinearLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }

        val bannerHeightPx = dpToPx(context, carouselWithoutPaging.content.config.bannerHeight)
        val width = carouselWithoutPaging.content.config.ratio * bannerHeightPx / 100
        val radiusInPixels = dpToPx(context, carouselWithoutPaging.content.config.radius)

        // Add dummy ImageViews to the inner LinearLayout
        carouselWithoutPaging.content.data.forEachIndexed { index, data -> // Change the range for more or fewer images
            val imageView = ImageView(context).apply {
                setImageBitmap(data.image)
                layoutParams = LayoutParams(width, bannerHeightPx).apply { // Set width and height
                    when (index) {
                        0 -> {
                            setMargins(0, 0, 0, 0) // Set margins between images
                        }
                        else -> {
                            setMargins(dpToPx(context, carouselWithoutPaging.content.config.spacing), 0, 0, 0) // Set margins between images
                        }
                    }
                }
                scaleType = ImageView.ScaleType.CENTER_CROP // Scale type for images

                // 角丸の設定
                outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, radiusInPixels.toFloat())
                    }
                }
                clipToOutline = true

                // クリックイベントの設定
                setOnClickListener {
                    if (data.linkUrl.isEmpty()) return@setOnClickListener
                    iafTracker.trackClick(index, data.linkUrl)
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(data.linkUrl))
                    startActivity(context, intent, null)
                }
            }
            innerLinearLayout.addView(imageView) // Add ImageView to inner LinearLayout
        }

        // Add the inner LinearLayout to the HorizontalScrollView
        horizontalScrollView.addView(innerLinearLayout)

        // Finally, add the HorizontalScrollView to the main LinearLayout
        this.addView(horizontalScrollView)

        iafTracker.trackOpen()
    }
}
