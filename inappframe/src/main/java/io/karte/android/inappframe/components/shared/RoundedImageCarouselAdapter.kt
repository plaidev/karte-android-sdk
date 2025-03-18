package io.karte.android.inappframe.components.shared

import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import androidx.constraintlayout.helper.widget.Carousel
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import io.karte.android.inappframe.model.Image

internal open class RoundedImageCarouselAdapter(
    private val context: android.content.Context,
    private val resources: android.content.res.Resources,
    private val radius: Int,
    private val imageList: List<Image>
) : Carousel.Adapter, DensityConvertible {
    override fun count(): Int {
        return imageList.size
    }

    override fun populate(view: android.view.View, index: Int) {
        // FIXME: ここもtry catch必要な気がする
        val imageButton = view as ImageView

        // dpからpixelに変換
        val radiusInPixels = dpToPx(context, radius)

        imageButton.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, radiusInPixels.toFloat())
            }
        }
        imageButton.clipToOutline = true
        imageButton.scaleType = ImageView.ScaleType.CENTER_CROP

        val image = imageList[index].image
        RoundedBitmapDrawableFactory.create(resources, image).run {
            cornerRadius = radiusInPixels.toFloat()
            imageButton.setImageDrawable(this)
        }
    }

    override fun onNewItem(index: Int) {
    }
}
