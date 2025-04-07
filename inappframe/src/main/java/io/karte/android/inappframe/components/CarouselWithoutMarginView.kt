package io.karte.android.inappframe.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import io.karte.android.inappframe.InAppFrame
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewTreeObserver
import androidx.core.content.ContextCompat.startActivity
import io.karte.android.inappframe.R
import io.karte.android.inappframe.databinding.CarouselWithoutMarginBinding
import io.karte.android.inappframe.components.shared.AutoScrollableTransitionListener
import io.karte.android.inappframe.components.shared.AutoSpeedConfigurable
import io.karte.android.inappframe.model.CarouselWithoutMarginV1
import io.karte.android.inappframe.components.shared.TappableMotionLayout
import io.karte.android.inappframe.components.shared.DensityConvertible
import io.karte.android.inappframe.model.IAFTracker
import io.karte.android.inappframe.components.shared.RoundedImageCarouselAdapter

internal class CarouselWithoutMarginView private constructor(
    context: Context
) : TappableMotionLayout(context), DensityConvertible, AutoSpeedConfigurable {
    private val binding = CarouselWithoutMarginBinding.inflate(LayoutInflater.from(context), this, true)
    private lateinit var tracker: IAFTracker
    private lateinit var data: CarouselWithoutMarginV1
    private val motionLayoutView = binding.carouselWithoutMarginMotionLayout
    private val handler = Handler(Looper.getMainLooper())
    private val motionLayoutDuration = 1000
    private var userInteracted = false

    private val autoScrollRunnable = Runnable { motionLayoutView.transitionToEnd() }

    internal constructor(context: Context, carouselWithoutMargin: CarouselWithoutMarginV1, tracker: IAFTracker) : this(context) {
        data = carouselWithoutMargin
        this.tracker = tracker
        binding.carouselWithoutMarginCarousel.setAdapter(RoundedImageCarouselAdapter(context, resources, data.content.config.radius, data.content.data))
        val motionLayoutTransition = motionLayoutView.getTransition(R.id.carousel_without_margin_forward)
        data.content.config.autoplaySpeed.run {
            motionLayoutTransition.duration = motionLayoutDuration
        }
        motionLayoutView.setTransitionListener(AutoScrollableTransitionListener(handler, autoScrollRunnable, data.content.config.autoplaySpeed, userInteracted))

        val paddingTopInPixels = dpToPx(context, data.content.config.paddingTop)
        val paddingBottomInPixels = dpToPx(context, data.content.config.paddingBottom)
        motionLayoutView.setPadding(
            0,
            paddingTopInPixels,
            0,
            paddingBottomInPixels
        )
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        val motionLayoutView = binding.carouselWithoutMarginMotionLayout
        motionLayoutView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                motionLayoutView.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
        handler.removeCallbacks(autoScrollRunnable)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val imageViewIds = listOf(
            R.id.carousel_without_margin_imageView0,
            R.id.carousel_without_margin_imageView1,
            R.id.carousel_without_margin_imageView2
        )

        val motionLayoutView = binding.carouselWithoutMarginMotionLayout
        motionLayoutView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                motionLayoutView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val constraintSetIds = listOf(
                    R.id.carousel_without_margin_base_state,
                    R.id.carousel_without_margin_next,
                    R.id.carousel_without_margin_previous
                )

                val paddingTopInPixels = dpToPx(context, data.content.config.paddingTop)
                val paddingBottomInPixels = dpToPx(context, data.content.config.paddingBottom)

                val layoutWidth = motionLayoutView.width
                val calculatedHeight = (layoutWidth / (data.content.config.ratio / 100.0)).toInt()

                motionLayoutView.layoutParams = motionLayoutView.layoutParams.apply {
                    height = calculatedHeight + paddingTopInPixels + paddingBottomInPixels
                }

                constraintSetIds.forEach { constraintSetId ->
                    val constraintSet = motionLayoutView.getConstraintSet(constraintSetId)
                    imageViewIds.forEach { imageViewId ->
                        constraintSet.constrainWidth(imageViewId, layoutWidth)
                        constraintSet.constrainHeight(imageViewId, calculatedHeight)
                    }
                    motionLayoutView.updateState(constraintSetId, constraintSet)
                }
            }
        })

        getTransitionStopMillis(data.content.config.autoplaySpeed)?.run {
            handler.postDelayed(autoScrollRunnable, this)
        }
        tracker.trackOpen()
    }

    override fun onClickAction() {
        val currentIndex = binding.carouselWithoutMarginCarousel.currentIndex
        val linkUrl = data.content.data[currentIndex].linkUrl
        if (linkUrl.isEmpty()) return
        tracker.trackClick(currentIndex, linkUrl)

        val uri = Uri.parse(linkUrl)
        // InAppFrameからURLを処理すべきか確認
        if (InAppFrame.shouldHandleUrl(uri)) {
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(context, intent, null)
        }
    }
}
