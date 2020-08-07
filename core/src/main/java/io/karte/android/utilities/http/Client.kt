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
package io.karte.android.utilities.http

import io.karte.android.core.logger.Logger
import io.karte.android.utilities.asString
import io.karte.android.utilities.gzip
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

private const val LOG_TAG = "Karte.HttpClient"

/** サーバにHTTPリクエストを送信するオブジェクト。 */
object Client {

    /** [Request]インスタンスの送信を実行します。 */
    @Throws(IOException::class)
    fun execute(request: Request<*>): Response {
        val url: URL
        val conn: HttpURLConnection
        try {
            url = URL(request.url)
            conn = url.openConnection() as HttpURLConnection
            try {
                conn.readTimeout = request.timeout
                conn.connectTimeout = request.timeout

                for ((key, value) in request.headers) {
                    conn.setRequestProperty(key, value)
                }
                conn.doInput = true
                conn.useCaches = false
                conn.requestMethod = request.method
                if (request.hasBody) {
                    conn.doOutput = true
                    var written = false
                    if (request is JSONRequest) {
                        val tmpStream = ByteArrayOutputStream()
                        request.writeBody(tmpStream)
                        gzip(tmpStream.toString("UTF-8"))?.let {
                            conn.setRequestProperty(HEADER_CONTENT_ENCODING, CONTENT_ENCODING_GZIP)
                            conn.outputStream.write(it)
                            written = true
                        }
                    }
                    if (!written) {
                        request.writeBody(conn.outputStream)
                    }
                }

                conn.connect()
                BufferedInputStream(conn.inputStream).use {
                    return Response(conn.responseCode, conn.headerFields, it.asString())
                }
            } catch (ignore: FileNotFoundException) {
                // come here when response code >= 400
                // L176: https://android.googlesource.com/platform/libcore/+/757afaa7afe96791a3cc612c9e3c4597a7321c7e/luni/src/main/java/libcore/net/http/HttpURLConnectionImpl.java
                conn.errorStream.use {
                    return Response(conn.responseCode, conn.headerFields, it.asString())
                }
            } catch (e: IOException) {
                Logger.e(LOG_TAG, "Failed to send request.", e)
                throw e
            } finally {
                conn.disconnect()
            }
        } catch (e: IOException) {
            Logger.e(LOG_TAG, "Can't construct track url.", e)
            throw e
        }
    }
}
