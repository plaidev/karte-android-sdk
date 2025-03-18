package io.karte.android.inappframe

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import io.karte.android.inappframe.components.CarouselWithMarginView
import io.karte.android.inappframe.components.CarouselWithoutMarginView
import io.karte.android.inappframe.components.CarouselWithoutPagingView
import io.karte.android.inappframe.components.SimpleBannerView
import io.karte.android.inappframe.databinding.InappFrameBinding
import io.karte.android.inappframe.model.CarouselWithMarginV1
import io.karte.android.inappframe.model.CarouselWithoutMarginV1
import io.karte.android.inappframe.model.CarouselWithoutPagingV1
import io.karte.android.inappframe.model.Empty
import io.karte.android.inappframe.model.IAFTracker
import io.karte.android.inappframe.model.InAppFrameData
import io.karte.android.inappframe.model.InAppFrameDeserializer
import io.karte.android.inappframe.model.SimpleBannerV1
import io.karte.android.variables.Variables
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val IN_APP_FRAME_ROOT_LOG = "In-App Frame"
const val PREFIX = "KRT_IN_APP_FRAME$"

class InAppFrame : LinearLayout {
    private val placeId: String
    private var inAppFrameData: InAppFrameData? = null
    private var iafTracker: IAFTracker? = null

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        val attr = context.obtainStyledAttributes(attrs, R.styleable.InAppFrame, 0, 0)
        placeId = requireNotNull(attr.getString(R.styleable.InAppFrame_placeId))
        attr.recycle()
    }

    constructor(context: Context, placeId: String) : super(context, null) {
        this.placeId = placeId
    }

    private val inAppFrameWrapper = InappFrameBinding.inflate(LayoutInflater.from(context), this, true).inAppFrameWrapper

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // 事前にデシリアライズされたデータがある場合は、それを使用
        if (inAppFrameData != null && iafTracker != null) {
            CoroutineScope(Dispatchers.Main).launch {
                renderIAF(inAppFrameData!!, iafTracker!!)
            }
            return
        }

        // 従来の処理
        runCatching {
            val variable = Variables.get("$PREFIX$placeId")
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val (inAppFrame, iafTracker) = InAppFrameDeserializer.deserialize(variable)
                    renderIAF(inAppFrame, iafTracker)
                } catch (e: Exception) {
                    Log.d(IN_APP_FRAME_ROOT_LOG, "render failed $e")
                    removeSelf()
                }
            }
        }.onFailure {
            Log.d(IN_APP_FRAME_ROOT_LOG, "init failed")
            removeSelf()
        }
    }

    private fun removeSelf() {
        (parent as ViewGroup?)?.removeView(this)
    }

    private suspend fun renderIAF(inAppFrame: InAppFrameData, tracker: IAFTracker): Unit = withContext(Dispatchers.Main) {
        inAppFrameWrapper.removeAllViews()
        when (inAppFrame) {
            is CarouselWithMarginV1 -> inAppFrameWrapper.addView(CarouselWithMarginView(context, inAppFrame, tracker))
            is CarouselWithoutMarginV1 -> inAppFrameWrapper.addView(CarouselWithoutMarginView(context, inAppFrame, tracker))
            is CarouselWithoutPagingV1 -> inAppFrameWrapper.addView(CarouselWithoutPagingView(context, inAppFrame, tracker))
            is SimpleBannerV1 -> inAppFrameWrapper.addView(SimpleBannerView(context, inAppFrame, tracker))
            is Empty -> {
                throw Exception("emptyView")
            }
        }
    }
    companion object
}
