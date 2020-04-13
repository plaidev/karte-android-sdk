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
package io.karte.android.visualtracking.internal

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.UiThread
import io.karte.android.KarteApp
import io.karte.android.KarteServerException
import io.karte.android.core.logger.Logger
import io.karte.android.utilities.ActivityLifecycleCallback
import io.karte.android.utilities.http.CONTENT_TYPE_JSON
import io.karte.android.utilities.http.CONTENT_TYPE_OCTET_STREAM
import io.karte.android.utilities.http.CONTENT_TYPE_TEXT
import io.karte.android.utilities.http.Client
import io.karte.android.utilities.http.HEADER_APP_KEY
import io.karte.android.utilities.http.HEADER_CONTENT_TYPE
import io.karte.android.utilities.http.JSONRequest
import io.karte.android.utilities.http.METHOD_POST
import io.karte.android.utilities.http.MultipartRequest
import io.karte.android.utilities.http.Response
import org.json.JSONException
import org.json.JSONObject
import java.util.ArrayList
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

private const val LOG_TAG = "Karte.ATPairing"

private const val HEADER_ACCOUNT_ID = "X-KARTE-Auto-Track-Account-Id"

private const val ENDPOINT_PAIRING_START = "/auto-track/pairing-start"
private const val ENDPOINT_PAIRING_HEARTBEAT = "/auto-track/pairing-heartbeat"
private const val ENDPOINT_POST_TRACE = "/auto-track/trace"

private const val RESPONSE_INVALID_STATE = "invalid_state"
private const val RESPONSE_BODY_TYPE = "type"

internal class PairingManager(private val app: KarteApp): ActivityLifecycleCallback() {
    private val traceSendExecutor = Executors.newCachedThreadPool()
    private val pollingExecutor = Executors.newScheduledThreadPool(1)
    private var pairingAccountId: String? = null
    private val isInPairing: Boolean
        get() = pairingAccountId != null

    fun startPairing(accountId: String, context: Context) {
        app.application.registerActivityLifecycleCallbacks(this)

        if (isInPairing) return

        pollingExecutor.execute {
            try {
                val url = app.config.baseUrl + ENDPOINT_PAIRING_START

                val json = JSONObject()
                    .put("os", "android")
                    .put("app_info", app.appInfo?.json)
                    .put("visitor_id", KarteApp.visitorId)

                val request = JSONRequest(url, METHOD_POST).apply { body = json.toString() }
                request.headers[HEADER_APP_KEY] = app.appKey
                request.headers[HEADER_CONTENT_TYPE] = CONTENT_TYPE_JSON
                request.headers[HEADER_ACCOUNT_ID] = accountId

                val resp = Client.execute(request)
                if (resp.isSuccessful) {
                    setPairingAccountId(accountId)
                    Logger.i(LOG_TAG, "Started pairing. accountId=$accountId")
                    startHeartBeat(accountId)
                } else {
                    throw KarteServerException(resp.body)
                }
            } catch (e: Exception) {
                showPairingFailedToast(context)
                setPairingAccountId(null)
                Logger.e(LOG_TAG, "Failed to start Pairing.", e)
            }
        }
    }

    private fun startHeartBeat(accountId: String) {
        pollingExecutor.schedule(object : Runnable {
            override fun run() {
                if (!isInPairing) return
                try {
                    val url = app.config.baseUrl + ENDPOINT_PAIRING_HEARTBEAT

                    val json = JSONObject().put("visitor_id", KarteApp.visitorId)
                    val request =
                        JSONRequest(url, METHOD_POST).apply { this.body = json.toString() }
                    request.headers[HEADER_APP_KEY] = app.appKey
                    request.headers[HEADER_CONTENT_TYPE] = CONTENT_TYPE_JSON
                    request.headers[HEADER_ACCOUNT_ID] = accountId

                    val res = Client.execute(request)
                    finishPairingIfNeeded(res)
                } catch (e: Exception) {
                    Logger.e(LOG_TAG, "Failed to heartbeat.", e)
                }

                if (isInPairing) pollingExecutor.schedule(this, 5, TimeUnit.SECONDS)
            }
        }, 5, TimeUnit.SECONDS)
    }

    @UiThread
    internal fun sendTraceIfInPairing(trace: Trace) {
        if (!isInPairing) return
        val traceValues = trace.values
        trace.getBitmapIfNeeded { bitmap ->
            sendTraceInternal(traceValues, bitmap)
        }
    }

    private fun sendTraceInternal(trace: JSONObject, bitmap: Bitmap?) {
        traceSendExecutor.execute(Runnable {
            try {

                val parts = ArrayList<MultipartRequest.Part<*>>()
                val traceBody = JSONObject()
                    .put("os", "android")
                    .put("visitor_id", KarteApp.visitorId)
                    .put("values", trace)

                val valuesPart = MultipartRequest.StringPart("trace", traceBody.toString())
                valuesPart.headers[HEADER_CONTENT_TYPE] = CONTENT_TYPE_TEXT
                parts.add(valuesPart)

                if (bitmap != null) {
                    val imagePart = MultipartRequest.BitmapPart("image", bitmap)
                    imagePart.headers[HEADER_CONTENT_TYPE] = CONTENT_TYPE_OCTET_STREAM
                    parts.add(imagePart)
                }

                val url = app.config.baseUrl + ENDPOINT_POST_TRACE
                val request = MultipartRequest(url, METHOD_POST, parts)
                request.headers[HEADER_APP_KEY] = app.appKey

                if (pairingAccountId == null) return@Runnable
                pairingAccountId?.let { request.headers[HEADER_ACCOUNT_ID] = it }

                val res = Client.execute(request)
                if (res.isSuccessful) {
                    Logger.i(LOG_TAG, "Sent action=" + trace.getString("action"))
                } else {
                    Logger.e(LOG_TAG, "Failed to send action. Response=" + res.body)
                }
                finishPairingIfNeeded(res)
            } catch (e: Exception) {
                Logger.e(LOG_TAG, "Failed to send action info.", e)
            }
        })
    }

    private fun showPairingFailedToast(context: Context) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                context,
                "ペアリングに失敗しました",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    @Throws(JSONException::class)
    private fun finishPairingIfNeeded(response: Response) {
        app.application.unregisterActivityLifecycleCallbacks(this)

        if (!response.isSuccessful) {
            val resBody = JSONObject(response.body)
            if (RESPONSE_INVALID_STATE == resBody.optString(RESPONSE_BODY_TYPE)) {
                setPairingAccountId(null)
                Logger.i(LOG_TAG, "Finish pairing.")
            }
        }
    }

    private fun setPairingAccountId(pairingAccountId: String?) {
        this.pairingAccountId = pairingAccountId
    }

    override fun onActivityStarted(activity: Activity) {
        if ((pollingExecutor as ScheduledThreadPoolExecutor).taskCount > 0) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
