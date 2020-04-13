//
//  Copyright 2020 PLAID, Inc.
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//      https://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//
package io.karte.android.notifications.internal

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import io.karte.android.core.logger.Logger
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.MalformedURLException
import java.net.URL

private const val LOG_TAG = "Karte.BitmapUtil"

internal object BitmapUtil {

    fun getBigPicture(path: String): Bitmap? {
        val metrics = Resources.getSystem().displayMetrics
        return getBitmapFromURL(path, metrics.widthPixels, metrics.heightPixels)
    }

    /**
     * Download a bitmap.
     * @param url The string of URL image.
     * @return The bitmap downloaded from URL.
     */
    private fun getBitmapFromURL(url: String, maxWidth: Int, maxHeight: Int): Bitmap? {
        var dstBitmap: Bitmap? = null
        var stream: InputStream? = null
        try {
            stream = URL(url).openStream()
            val out = ByteArrayOutputStream()
            val buff = ByteArray(4096)
            var n = 0
            while (stream.read(buff).also { n = it } > 0) {
                out.write(buff, 0, n)
            }
            val rawData = out.toByteArray()

            val opts = BitmapFactory.Options()
            opts.inJustDecodeBounds = true
            BitmapFactory.decodeByteArray(rawData, 0, rawData.size, opts)

            opts.inSampleSize = BitmapUtil.calculateInSampleSize(opts, maxWidth, maxHeight)
            opts.inJustDecodeBounds = false
            val srcBitmap =
                BitmapFactory.decodeByteArray(rawData, 0, rawData.size, opts)

            // Nexus5でGIF画像が表示されない問題に対するワークアラウンド (再レンダリングすると表示される)
            dstBitmap =
                Bitmap.createBitmap(srcBitmap.width, srcBitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(dstBitmap!!)
            canvas.drawBitmap(srcBitmap, 0f, 0f, Paint())
        } catch (e: MalformedURLException) {
            Logger.e(LOG_TAG, String.format("Invalid URL: %s", url), e)
        } catch (e: IOException) {
            Logger.e(LOG_TAG, String.format("IOException in image download for URL: %s.", url), e)
        } catch (e: OutOfMemoryError) {
            Logger.e(LOG_TAG, String.format("OutOfMemoryError in image download for URL: %s.", url), e)
            dstBitmap = null
        } finally {
            closeStream(stream)
        }
        return dstBitmap
    }

    /**
     * Method to close InputStream.
     *
     * @param inputStream The InputStream which must be closed.
     */
    private fun closeStream(inputStream: InputStream?) {
        try {
            inputStream?.close()
        } catch (e: IOException) {
            Logger.e(LOG_TAG, "IOException during closing of image download stream.", e)
        }
    }

    /**
     * calculates InSampleSize which can be passed to BitmapFactory's decode methods to cap the height and width of the image.
     * see https://developer.android.com/topic/performance/graphics/load-bitmap#load-bitmap for details.
     * @param options The BitmapFactory.options that stores outHeight and outWidth of target image.
     * @param maxWidth Required width of the resulting image.
     * @param maxHeight Required width of the resulting image.
     * @return inSampleSize
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        maxWidth: Int,
        maxHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        // height,widthがそれぞれmaxHeight, maxWidth以下になる最小のinSampleSize（2のpower）を探す。
        if (height > maxHeight || width > maxWidth) {
            while (true) {
                inSampleSize *= 2
                if (height / inSampleSize <= maxHeight && width / inSampleSize <= maxWidth) {
                    break
                }
            }
        }
        return inSampleSize
    }
}
