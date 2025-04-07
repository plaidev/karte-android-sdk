package io.karte.android.inappframe.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import io.karte.android.inappframe.InAppFrame
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewTreeObserver
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat.startActivity
import io.karte.android.inappframe.R
import io.karte.android.inappframe.databinding.CarouselWithMarginBinding
import io.karte.android.inappframe.model.CarouselWithMarginV1
import io.karte.android.inappframe.components.shared.AutoScrollableTransitionListener
import io.karte.android.inappframe.components.shared.AutoSpeedConfigurable
import io.karte.android.inappframe.components.shared.TappableMotionLayout
import io.karte.android.inappframe.components.shared.DensityConvertible
import io.karte.android.inappframe.model.IAFTracker
import io.karte.android.inappframe.components.shared.RoundedImageCarouselAdapter

internal class CarouselWithMarginView private constructor(context: Context) : TappableMotionLayout(context), DensityConvertible, AutoSpeedConfigurable {
    private val binding = CarouselWithMarginBinding.inflate(LayoutInflater.from(context), this, true)

    private lateinit var data: CarouselWithMarginV1
    private lateinit var tracker: IAFTracker

    private var isConstraintGuidesSet = false

    private var handler = Handler(Looper.getMainLooper())

    private val motionLayoutDuration = 1000
    private var userInteracted = false
    private val motionLayoutView = binding.carouselWithMarginMotionLayout

    private val autoScrollRunnable = Runnable { motionLayoutView.transitionToEnd() }

    internal constructor(context: Context, carouselWithMargin: CarouselWithMarginV1, tracker: IAFTracker) : this(context) {
        data = carouselWithMargin
        this.tracker = tracker
        binding.carouselWithMarginCarousel.setAdapter(RoundedImageCarouselAdapter(context, resources, data.content.config.radius, data.content.data))

        val motionLayoutTransition = motionLayoutView.getTransition(R.id.carousel_with_margin_forward)
        data.content.config.autoplaySpeed.run {
            motionLayoutTransition.duration = motionLayoutDuration
        }
        motionLayoutView.setTransitionListener(AutoScrollableTransitionListener(handler, autoScrollRunnable, data.content.config.autoplaySpeed, userInteracted))

        motionLayoutView.layoutParams = motionLayoutView.layoutParams.apply {
            height = dpToPx(context, data.content.config.bannerHeight) +
                dpToPx(context, data.content.config.paddingTop) +
                dpToPx(context, data.content.config.paddingBottom)
        }

        val imageViewIds = listOf(
            R.id.carousel_with_margin_imageView0,
            R.id.carousel_with_margin_imageView1,
            R.id.carousel_with_margin_imageView2,
            R.id.carousel_with_margin_imageView3,
            R.id.carousel_with_margin_imageView4
        )

        val motionLayoutView = binding.carouselWithMarginMotionLayout
        motionLayoutView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {

            override fun onGlobalLayout() {
                motionLayoutView.viewTreeObserver.removeOnGlobalLayoutListener(this)

                val constraintSetIds = listOf(
                    R.id.carousel_with_margin_next,
                    R.id.carousel_with_margin_previous,
                    R.id.carousel_with_margin_base_state
                )
                val layoutWidth = dpToPx(context, (data.content.config.bannerHeight * (data.content.config.ratio / 100.0)).toInt())
                val calculatedHeight = dpToPx(context, data.content.config.bannerHeight)
                val spacingInPixels = dpToPx(context, data.content.config.spacing)

                constraintSetIds.forEach { constraintSetId ->
                    val constraintSet = motionLayoutView.getConstraintSet(constraintSetId)
                    imageViewIds.forEachIndexed { _, imageViewId ->
                        constraintSet.constrainWidth(imageViewId, layoutWidth)
                        constraintSet.constrainHeight(imageViewId, calculatedHeight)

                        if (constraintSetId == R.id.carousel_with_margin_base_state) {
                            if (imageViewId == R.id.carousel_with_margin_imageView0 || imageViewId == R.id.carousel_with_margin_imageView1) {
                                constraintSet.setMargin(imageViewId, ConstraintSet.END, spacingInPixels)
                            }
                            if (imageViewId == R.id.carousel_with_margin_imageView3 || imageViewId == R.id.carousel_with_margin_imageView4) {
                                constraintSet.setMargin(imageViewId, ConstraintSet.START, spacingInPixels)
                            }
                        } else if (constraintSetId == R.id.carousel_with_margin_next) {
                            if (imageViewId == R.id.carousel_with_margin_imageView0 || imageViewId == R.id.carousel_with_margin_imageView1 || imageViewId == R.id.carousel_with_margin_imageView2) {
                                constraintSet.setMargin(imageViewId, ConstraintSet.END, spacingInPixels)
                            }
                            if (imageViewId == R.id.carousel_with_margin_imageView4) {
                                constraintSet.setMargin(imageViewId, ConstraintSet.START, spacingInPixels)
                            }
                        } else if (constraintSetId == R.id.carousel_with_margin_previous) {
                            if (imageViewId == R.id.carousel_with_margin_imageView0) {
                                constraintSet.setMargin(imageViewId, ConstraintSet.END, spacingInPixels)
                            }
                            if (imageViewId == R.id.carousel_with_margin_imageView2 || imageViewId == R.id.carousel_with_margin_imageView3 || imageViewId == R.id.carousel_with_margin_imageView4) {
                                constraintSet.setMargin(imageViewId, ConstraintSet.START, spacingInPixels)
                            }
                        }
                    }
                    motionLayoutView.updateState(constraintSetId, constraintSet)
                }
            }
        })
        getTransitionStopMillis(data.content.config.autoplaySpeed)?.run {
            handler.postDelayed(autoScrollRunnable, this)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (isConstraintGuidesSet) return

        // constraintGuideの計算を今の画面幅から計算している。
        val measuredWidth = MeasureSpec.getSize(widthMeasureSpec)
        val layoutWidth = dpToPx(context, (data.content.config.bannerHeight * (data.content.config.ratio / 100.0)).toInt())
        val spacingInPixels = dpToPx(context, data.content.config.spacing)

        // Composeの実装を参考に、画面幅とカルーセル幅の差を計算し、負の値にならないようにする
        val availableSpace = measuredWidth - layoutWidth
        // 利用可能なスペースの半分から、マージンの半分を引いた値（最小値は0）
        val guidelineConstraint = maxOf(0, availableSpace / 2 - spacingInPixels / 2)

        binding.carouselWithMarginGuideline.layoutParams = (binding.carouselWithMarginGuideline.layoutParams as LayoutParams).apply {
            guideBegin = guidelineConstraint
        }

        binding.carouselWithMarginGuideline2.layoutParams = (binding.carouselWithMarginGuideline2.layoutParams as LayoutParams).apply {
            guideEnd = guidelineConstraint
        }

        isConstraintGuidesSet = true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        tracker.trackOpen()
    }

    override fun onClickAction() {
        val currentIndex = binding.carouselWithMarginCarousel.currentIndex
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
