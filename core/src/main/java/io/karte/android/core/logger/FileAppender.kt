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

package io.karte.android.core.logger

import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.util.Log
import io.karte.android.BuildConfig
import io.karte.android.KarteApp
import io.karte.android.utilities.http.CONTENT_TYPE_TEXT
import io.karte.android.utilities.http.Client
import io.karte.android.utilities.http.HEADER_APP_KEY
import io.karte.android.utilities.http.HEADER_CONTENT_TYPE
import io.karte.android.utilities.http.JSONRequest
import io.karte.android.utilities.http.METHOD_POST
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.Flushable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

internal object Clock {
    fun now(): Date = Date()
}

private fun Date.format(pattern: String): String =
    SimpleDateFormat(pattern, Locale.getDefault()).format(this)

private fun Date.forLog(): String = format("yyyy-MM-dd HH:mm:ss")
private fun Date.asPrefix(): String = format("yyyy-MM-dd")
private fun File.files(): List<File> = listFiles()?.filter { it.isFile } ?: listOf()
private const val LOG_TAG = "Karte.Log.FileAppender"
internal const val THREAD_NAME = "io.karte.android.logger.buffer"
private const val BUFFER_SIZE = 10000

private fun logDebug(message: String) {
    if (!BuildConfig.DEBUG) return
    if (Logger.level > LogLevel.DEBUG) return
    Log.d(LOG_TAG, message)
}

internal class FileAppender : Appender, Flushable {
    private val handler: Handler =
        Handler(HandlerThread(THREAD_NAME, Process.THREAD_PRIORITY_LOWEST).apply { start() }.looper)
    private val buffer = StringBuilder()

    private val logDir: File
        get() = File(KarteApp.self.application.cacheDir, "io.karte.android/log").apply { mkdirs() }

    /**Bufferの書き込み先ファイル.*/
    private val cacheFile: File
        get() {
            val date = Clock.now()
            val prefix = date.asPrefix()
            return logDir.files().filter { it.name.startsWith(prefix) }.maxBy { it.name }
                ?: File(logDir, "${prefix}_${date.time}.log")
        }

    /** upload対象ファイル. 当日分以外のファイル*/
    private val collectingFiles: List<File>
        get() = logDir.files().filterNot { it.name.startsWith(Clock.now().asPrefix()) }

    /** 3日より前のファイル */
    private val garbageFiles: List<File>
        get() {
            val prefix = Calendar.getInstance().apply {
                time = Clock.now()
                add(Calendar.DATE, -3)
            }.time.asPrefix()
            return logDir.files().filter { it.name < prefix }
        }

    override fun append(log: LogEvent) {
        val date = Clock.now()
        val tid = Process.myTid()
        handler.post {
            buffer.appendln(Layout.layout(date, tid, log))
            if (buffer.length > BUFFER_SIZE) write()
        }
    }

    override fun flush() {
        handler.post {
            write()
            Collector.collect(collectingFiles)
            cleanup()
        }
    }

    private fun write() {
        FileOutputStream(cacheFile, true).use { outputStream ->
            outputStream.channel.lock().use {
                outputStream.write(buffer.toString().toByteArray())
                buffer.setLength(0)
            }
        }
    }

    private fun cleanup() {
        val files = garbageFiles
        logDebug("cleanup ${files.size}")
        files.forEach { it.delete() }
    }
}

private object Layout {
    fun layout(date: Date, tid: Int, log: LogEvent): String {
        val stacktrace =
            if (log.throwable != null) "\n" + Log.getStackTraceString(log.throwable) else ""
        return "${date.forLog()} ${Process.myPid()}-$tid " +
            "${priorityInitial(log.level)}/${log.tag} " +
            "${log.message}$stacktrace"
    }

    private fun priorityInitial(priority: LogLevel): String {
        return when (priority) {
            LogLevel.VERBOSE -> "V"
            LogLevel.DEBUG -> "D"
            LogLevel.INFO -> "I"
            LogLevel.WARN -> "W"
            LogLevel.ERROR -> "E"
        }
    }
}

private object Collector {
    /** 同期的/直列にファイルをアップロードする。 */
    fun collect(files: List<File>) {
        logDebug("start upload ${files.size}")
        files.forEach { file ->
            val uploadUrl = getUploadUrl(file) ?: return@forEach
            if (uploadUrl.isEmpty()) {
                // urlが送られてこなければアップロードしない.
                file.delete()
                return@forEach
            }

            val isSuccessful = uploadLog(file, uploadUrl)
            if (isSuccessful) file.delete()
        }
    }

    private fun getUploadUrl(file: File): String? {
        logDebug("get upload url $file")
        val request = JSONRequest(KarteApp.self.config.logCollectionUrl, METHOD_POST)
            .apply {
                headers[HEADER_APP_KEY] = KarteApp.self.appKey
                body = JSONObject()
                    .put("visitor_id", KarteApp.visitorId)
                    .put("local_date", file.name.split("_").first())
                    .toString()
            }
        return runCatching {
            val response = Client.execute(request)
            JSONObject(response.body).optString("url")
        }.getOrElse {
            logDebug("request was failed: $it")
            return null
        }
    }

    private fun uploadLog(file: File, url: String): Boolean {
        logDebug("start upload $url")
        val request = JSONRequest(url, "PUT")
            .apply {
                headers[HEADER_CONTENT_TYPE] = CONTENT_TYPE_TEXT
                timeout = 120000
                file.inputStream().use { inputStream ->
                    inputStream.bufferedReader().use { reader ->
                        body = reader.readText()
                    }
                }
            }
        val response = runCatching { Client.execute(request) }.getOrNull()
        logDebug("uploaded response $response")
        return response?.isSuccessful == true
    }
}
