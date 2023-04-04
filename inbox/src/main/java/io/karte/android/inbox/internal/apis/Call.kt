package io.karte.android.inbox.internal.apis

import io.karte.android.core.logger.Logger
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection

private const val LOG_TAG = "Karte.Inbox.Call"

internal class Call(private val request: BaseApiRequest) {
    internal fun execute(): JSONObject? {
        var raw: JSONObject? = null

        try {
            buildConnection(request).run {
                try {
                    connect()
                    raw = handleResponse(this)
                } catch (e: IOException) {
                    Logger.e(LOG_TAG, "Failed to get response.", e)
                } finally {
                    disconnect()
                }
            }
        } catch (e: IOException) {
            Logger.e(LOG_TAG, "Failed to construct connection.", e)
        }
        return raw
    }

    @Throws(IOException::class)
    private fun buildConnection(request: BaseApiRequest): HttpURLConnection {
        return (request.url.openConnection() as HttpURLConnection).apply {
            for ((k, v) in request.header) {
                setRequestProperty(k, v)
            }

            requestMethod = request.method.name
            if (request.hasBody) {
                doOutput = true
                val out = BufferedWriter(OutputStreamWriter(outputStream))
                out.write(request.body.toString())
                out.close()
            }
        }
    }

    private fun handleResponse(conn: HttpURLConnection): JSONObject? {
        val raw = runCatching {
            when (conn.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    conn.inputStream.bufferedReader().use {
                        JSONObject(it.readText())
                    }
                }
                HttpURLConnection.HTTP_BAD_REQUEST,
                HttpURLConnection.HTTP_UNAUTHORIZED,
                HttpURLConnection.HTTP_INTERNAL_ERROR -> {
                    val e = parseError(conn.errorStream)
                    Logger.e(LOG_TAG, "Response error: ${conn.responseCode}: $e")
                    null
                }
                else -> {
                    Logger.e(LOG_TAG, "Invalid response: ${conn.responseCode}")
                    null
                }
            }
        }
        return raw.getOrNull()
    }

    private fun parseError(stream: InputStream): String? {
        return runCatching {
            val e = JSONObject(stream.bufferedReader().readText())
            return e.optString("error")
        }.getOrNull()
    }
}
