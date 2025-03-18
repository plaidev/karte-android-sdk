package io.karte.android.inappframe.unit

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.content.ContextCompat
import io.karte.android.inappframe.components.CarouselWithoutMarginView
import io.karte.android.inappframe.R
import io.karte.android.inappframe.components.SimpleBannerView
import io.karte.android.inappframe.components.shared.DensityConvertible

/**
 * Extension functions for testing views
 */

/**
 * Sets up SimpleBannerView for testing without waiting for layout
 */
internal fun SimpleBannerView.setupViewForTesting() {
    // Use the device's display width for testing
    val displayWidth = context.resources.displayMetrics.widthPixels

    // Access private fields via reflection
    val contentField = this::class.java.getDeclaredField("content").apply { isAccessible = true }
    val content = contentField.get(this)

    val trackerField = this::class.java.getDeclaredField("tracker").apply { isAccessible = true }
    val tracker = trackerField.get(this)

    // Access config fields
    val configField = content::class.java.getDeclaredField("config").apply { isAccessible = true }
    val config = configField.get(content)

    val paddingStartField = config::class.java.getDeclaredField("paddingStart").apply { isAccessible = true }
    val paddingEndField = config::class.java.getDeclaredField("paddingEnd").apply { isAccessible = true }
    val ratioField = config::class.java.getDeclaredField("ratio").apply { isAccessible = true }
    val radiusField = config::class.java.getDeclaredField("radius").apply { isAccessible = true }

    val paddingStart = paddingStartField.getInt(config)
    val paddingEnd = paddingEndField.getInt(config)
    val ratio = ratioField.getInt(config)
    val radius = radiusField.getFloat(config)

    // Convert dp to px
    val dpToPxMethod = DensityConvertible::class.java.getDeclaredMethod("dpToPx", android.content.Context::class.java, Int::class.java)
    dpToPxMethod.isAccessible = true
    val paddingStartPx = dpToPxMethod.invoke(this, context, paddingStart) as Int
    val paddingEndPx = dpToPxMethod.invoke(this, context, paddingEnd) as Int

    // Calculate dimensions
    val availableWidth = displayWidth - paddingStartPx - paddingEndPx
    val height = (availableWidth * 100) / ratio

    // Create CardView
    val cardView = CardView(context).apply {
        this.radius = radius
        layoutParams = ViewGroup.LayoutParams(availableWidth, height)

        // Set click listener
        setOnClickListener {
            val dataField = content::class.java.getDeclaredField("data").apply { isAccessible = true }
            val data = dataField.get(content) as List<*>
            val firstItem = data.getOrNull(0)
            if (firstItem != null) {
                val linkUrlField = firstItem::class.java.getDeclaredField("linkUrl").apply { isAccessible = true }
                val linkUrl = linkUrlField.get(firstItem) as String
                if (linkUrl.isNotEmpty()) {
                    // Track click
                    val trackClickMethod = tracker::class.java.getDeclaredMethod("trackClick", Int::class.java, String::class.java)
                    trackClickMethod.invoke(tracker, 0, linkUrl)

                    // Start intent
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(linkUrl))
                    ContextCompat.startActivity(context, intent, null)
                }
            }
        }
    }

    // Create ImageView
    val createImageViewMethod = this::class.java.getDeclaredMethod("createImageView", content::class.java)
    createImageViewMethod.isAccessible = true
    val imageView = createImageViewMethod.invoke(this, content) as ImageView
    cardView.addView(imageView)

    // Add CardView to SimpleBannerView
    this.removeAllViews()
    this.addView(cardView)

    // Track open event
    val trackOpenMethod = tracker::class.java.getDeclaredMethod("trackOpen")
    trackOpenMethod.invoke(tracker)
}

/**
 * Sets up CarouselWithoutMarginView for testing without waiting for layout
 */
internal fun CarouselWithoutMarginView.setupViewForTesting() {
    // Set a default width for testing
    val defaultWidth = 1080 // Common screen width in pixels

    // Access private fields via reflection
    val dataField = this::class.java.getDeclaredField("data").apply { isAccessible = true }
    val data = dataField.get(this)

    val trackerField = this::class.java.getDeclaredField("tracker").apply { isAccessible = true }
    val tracker = trackerField.get(this)

    val motionLayoutViewField = this::class.java.getDeclaredField("motionLayoutView").apply { isAccessible = true }
    val motionLayoutView = motionLayoutViewField.get(this) as MotionLayout

    // Access content and config
    val contentField = data::class.java.getDeclaredField("content").apply { isAccessible = true }
    val content = contentField.get(data)

    val configField = content::class.java.getDeclaredField("config").apply { isAccessible = true }
    val config = configField.get(content)

    // Access config fields
    val paddingTopField = config::class.java.getDeclaredField("paddingTop").apply { isAccessible = true }
    val paddingBottomField = config::class.java.getDeclaredField("paddingBottom").apply { isAccessible = true }
    val ratioField = config::class.java.getDeclaredField("ratio").apply { isAccessible = true }

    val paddingTop = paddingTopField.getInt(config)
    val paddingBottom = paddingBottomField.getInt(config)
    val ratio = ratioField.getDouble(config)

    // Define image view and constraint set IDs
    val imageViewIds = listOf(
        R.id.carousel_without_margin_imageView0,
        R.id.carousel_without_margin_imageView1,
        R.id.carousel_without_margin_imageView2
    )

    val constraintSetIds = listOf(
        R.id.carousel_without_margin_base_state,
        R.id.carousel_without_margin_next,
        R.id.carousel_without_margin_previous
    )

    // Convert dp to px
    val dpToPxMethod = DensityConvertible::class.java.getDeclaredMethod("dpToPx", android.content.Context::class.java, Int::class.java)
    dpToPxMethod.isAccessible = true
    val paddingTopInPixels = dpToPxMethod.invoke(this, context, paddingTop) as Int
    val paddingBottomInPixels = dpToPxMethod.invoke(this, context, paddingBottom) as Int

    // Calculate height
    val calculatedHeight = (defaultWidth / (ratio / 100.0)).toInt()

    // Set layout params
    motionLayoutView.layoutParams = motionLayoutView.layoutParams.apply {
        height = calculatedHeight + paddingTopInPixels + paddingBottomInPixels
    }

    // Update constraint sets
    constraintSetIds.forEach { constraintSetId ->
        val constraintSet = motionLayoutView.getConstraintSet(constraintSetId)
        imageViewIds.forEach { imageViewId ->
            constraintSet.constrainWidth(imageViewId, defaultWidth)
            constraintSet.constrainHeight(imageViewId, calculatedHeight)
        }
        motionLayoutView.updateState(constraintSetId, constraintSet)
    }

    // Track open event
    val trackOpenMethod = tracker::class.java.getDeclaredMethod("trackOpen")
    trackOpenMethod.invoke(tracker)
}
