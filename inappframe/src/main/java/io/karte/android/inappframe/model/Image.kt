package io.karte.android.inappframe.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.json.JSONObject
import java.net.URL

internal data class Image(val linkUrl: String, val image: Bitmap?, val index: Int) {
    companion object {
        // メモリキャッシュの設定（LruCache を利用）
        private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        private val cacheSize = maxMemory / 8 // 利用可能なメモリの1/8をキャッシュサイズに設定
        private val imageCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, value: Bitmap): Int {
                // Bitmap のサイズをキロバイト単位で計算
                return value.byteCount / 1024
            }
        }

        suspend fun parseToListOrThrow(jsonObject: JSONObject): List<Image> = coroutineScope {
            val data = jsonObject.getJSONObject("content").getJSONArray("data")
            (0 until data.length()).map {
                async {
                    data.getJSONObject(it).run {
                        val imageUrl = getString("imageUrl")
                        val bitmap = fetchImage(imageUrl)
                        Image(
                            getString("linkUrl"),
                            bitmap,
                            getInt("index")
                        )
                    }
                }
            }.awaitAll()
        }

        private fun fetchImage(url: String): Bitmap? {
            // キャッシュからまず画像を取得する
            imageCache.get(url)?.let { return it }

            val connection = URL(url).openConnection()
            connection.connect()
            val inputStream = connection.getInputStream()
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            // 画像が取得できた場合、キャッシュに保存する
            bitmap?.also {
                imageCache.put(url, it)
            }
            return bitmap
        }
    }
}
