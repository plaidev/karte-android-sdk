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

import android.graphics.Bitmap
import java.io.BufferedOutputStream
import java.io.BufferedWriter
import java.io.DataOutputStream
import java.io.IOException
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.util.UUID

const val HEADER_CONTENT_TYPE = "Content-Type"
const val HEADER_CONTENT_ENCODING = "Content-Encoding"
const val CONTENT_ENCODING_GZIP = "gzip"
const val CONTENT_TYPE_TEXT = "text/plain; charset=utf-8"
const val CONTENT_TYPE_OCTET_STREAM = "application/octet-stream"
const val CONTENT_TYPE_JSON = "application/json"
const val HEADER_APP_KEY = "X-KARTE-App-Key"
private const val CRLF = "\r\n"

const val METHOD_POST = "POST"

abstract class Request<T> internal constructor(
    val url: String,
    val method: String,
    val headers: MutableMap<String, String> = mutableMapOf()
) {
    abstract var body: T?
    internal val hasBody: Boolean
        get() = body != null

    @Throws(IOException::class)
    internal abstract fun writeBody(outputStream: OutputStream)
}

open class JSONRequest(
    url: String,
    method: String
) : Request<String>(url, method) {
    override var body: String? = null

    init {
        headers[HEADER_CONTENT_TYPE] = CONTENT_TYPE_JSON
    }

    @Throws(IOException::class)
    override fun writeBody(outputStream: OutputStream) {
        body?.let { body ->
            BufferedWriter(OutputStreamWriter(outputStream, "UTF-8")).use {
                it.write(body)
            }
        }
    }
}

class MultipartRequest(url: String, method: String, override var body: List<Part<*>>?) :
    Request<List<MultipartRequest.Part<*>>>(url, method) {
    private val boundary: String = UUID.randomUUID().toString()

    init {
        headers[HEADER_CONTENT_TYPE] = "multipart/form-data; boundary=\"${this.boundary}\""
    }

    @Throws(IOException::class)
    override fun writeBody(outputStream: OutputStream) {
        val parts = body ?: return
        val dataOutputStream = DataOutputStream(BufferedOutputStream(outputStream))

        for (part in parts) {
            dataOutputStream.writeBytes("--$boundary$CRLF")
            for ((key, value) in part.headers) {
                dataOutputStream.writeBytes("$key: $value$CRLF")
            }
            dataOutputStream.writeBytes(CRLF)
            part.writeBody(dataOutputStream)
            dataOutputStream.writeBytes(CRLF)
        }

        dataOutputStream.writeBytes("--$boundary--$CRLF")
        dataOutputStream.close()
    }

    abstract class Part<T>(val name: String) {
        val headers: MutableMap<String, String> = HashMap()
        internal abstract val body: T
        internal abstract fun writeBody(outputStream: OutputStream)
    }

    class StringPart(name: String, override val body: String) : Part<String>(name) {
        init {
            headers["Content-Disposition"] = "form-data; name=\"$name\""
        }

        @Throws(IOException::class)
        override fun writeBody(outputStream: OutputStream) {
            outputStream.flush()
            // write with writer because string might be multi byte.
            val writer = OutputStreamWriter(outputStream)
            writer.write(body)
            writer.flush()
        }
    }

    class BitmapPart(name: String, override val body: Bitmap) : Part<Bitmap>(name) {
        init {
            headers["Content-Disposition"] = "form-data; name=\"$name\"; filename=\"$name\""
        }

        override fun writeBody(outputStream: OutputStream) {
            body.compress(Bitmap.CompressFormat.PNG, 70, outputStream)
        }
    }
}
