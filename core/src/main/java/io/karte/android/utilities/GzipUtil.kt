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
package io.karte.android.utilities

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

internal fun gzip(inputString: String?): ByteArray? {
    inputString ?: return null

    val bos = ByteArrayOutputStream()
    return runCatching { GZIPOutputStream(bos) }.getOrNull()?.let { stream ->
        stream.bufferedWriter(Charsets.UTF_8).use {
            it.write(inputString)
        }
        val byteArray = bos.toByteArray()
        if (isGzipped(byteArray)) {
            return byteArray
        }
        return null
    }
}

internal fun gunzip(bytes: ByteArray?): String? {
    if (!isGzipped(bytes)) return null

    return GZIPInputStream(bytes?.inputStream()).bufferedReader(Charsets.UTF_8).use {
        it.readText()
    }
}

private fun isGzipped(bytes: ByteArray?): Boolean {
    return if (bytes == null || bytes.size < 2) {
        false
    } else {
        bytes[0] == GZIPInputStream.GZIP_MAGIC.toByte() && bytes[1] == (GZIPInputStream.GZIP_MAGIC shr 8).toByte()
    }
}