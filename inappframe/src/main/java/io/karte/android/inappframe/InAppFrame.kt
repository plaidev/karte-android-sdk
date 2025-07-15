package io.karte.android.inappframe

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

const val IN_APP_FRAME_ROOT_LOG = "In-App Frame"
const val PREFIX = "KRT_IN_APP_FRAME$"

class InAppFrame : LinearLayout {
    private val placeId: String
    private val scope = MainScope()

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
        loadInAppFrameContent()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scope.cancel()
        Log.d(IN_APP_FRAME_ROOT_LOG, "InAppFrame detached and cleaned up")
    }

    private fun loadInAppFrameContent() {
        val variable = Variables.get("$PREFIX$placeId")

        scope.launch {
            try {
                coroutineScope {
                    val (inAppFrame, iafTracker) = InAppFrameDeserializer.deserialize(variable)
                    renderIAF(inAppFrame, iafTracker)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.d(IN_APP_FRAME_ROOT_LOG, "Failed to load InAppFrame content: $e")
                hideSelf()
            }
        }
    }

    private suspend fun hideSelf() = withContext(Dispatchers.Main) {
        // Viewを非表示にして安全に処理
        visibility = GONE
        Log.d(IN_APP_FRAME_ROOT_LOG, "InAppFrame hidden due to error")
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
    companion object {
        /**
         * InAppFrameのデリゲートオブジェクト
         */
        private var delegate: InAppFrameDelegate? = null

        /**
         * InAppFrameのデリゲートを設定します。
         * @param delegate デリゲート
         */
        @JvmStatic
        fun setDelegate(delegate: InAppFrameDelegate?) {
            this.delegate = delegate
        }

        /**
         * URLを処理するためのリスナーを取得します。
         * @param url 処理するURL
         * @return SDKがURLを処理すべき場合はtrue、そうでない場合はfalse
         */
        @JvmStatic
        internal fun shouldHandleUrl(url: Uri): Boolean {
            // デリゲートがある場合はそちらを優先
            delegate?.let {
                return it.shouldOpenURL(url)
            }
            // デフォルトではSDKが処理する
            return true
        }
    }
}
