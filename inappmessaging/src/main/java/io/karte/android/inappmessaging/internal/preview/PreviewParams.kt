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
package io.karte.android.inappmessaging.internal.preview

import android.app.Activity
import io.karte.android.KarteApp
import io.karte.android.core.logger.Logger
import org.json.JSONException
import org.json.JSONObject

private const val LOG_TAG = "Karte.PreviewParams"

internal class PreviewParams(activity: Activity) {
    private val shouldShowPreview = try {
        activity.intent?.data?.getQueryParameter("__krt_preview")
    } catch (e: Exception) {
        Logger.e(LOG_TAG, "Failed to get query parameter.", e)
        null
    }
    private val previewId = try {
        activity.intent?.data?.getQueryParameter("preview_id")
    } catch (e: Exception) {
        Logger.e(LOG_TAG, "Failed to get query parameter.", e)
        null
    }
    private val previewToken = try {
        activity.intent?.data?.getQueryParameter("preview_token")
    } catch (e: Exception) {
        Logger.e(LOG_TAG, "Failed to get query parameter.", e)
        null
    }

    fun shouldShowPreview(): Boolean {
        return shouldShowPreview != null && previewId != null && previewToken != null
    }

    private fun toJSON(): JSONObject? {
        return try {
            JSONObject()
                .put("preview_id", previewId)
                .put("preview_token", previewToken)
                .put("is_preview", "true")
        } catch (e: JSONException) {
            Logger.e(LOG_TAG, "Failed to construct json.", e)
            null
        }
    }

    fun generateUrl(app: KarteApp): String? {
        val karteOpts = toJSON() ?: return null

        return "${app.config.baseUrl}/overlay?app_key=${app.appKey}&_k_vid=${KarteApp.visitorId}" +
            "&_k_app_prof=${app.appInfo?.json}&__karte_opts=$karteOpts&__krtactionpreview=$previewToken"
    }
}
