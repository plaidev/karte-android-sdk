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
package io.karte.android.visualtracking.internal.tracking

import io.karte.android.KarteApp
import io.karte.android.core.logger.Logger
import io.karte.android.utilities.http.Client
import io.karte.android.utilities.http.HEADER_APP_KEY
import io.karte.android.utilities.http.JSONRequest
import org.json.JSONObject

private const val LOG_TAG = "Karte.VTDefinitions"
private const val ENDPOINT_GET_DEFINITIONS = "/auto-track/definitions"

internal object GetDefinitions {
    fun get(
        app: KarteApp,
        requestApply: ((JSONRequest) -> Unit)?,
        completion: ((JSONObject?) -> Unit)?
    ) {
        try {
            val request =
                JSONRequest(app.config.baseUrl + ENDPOINT_GET_DEFINITIONS, "GET")
            request.headers.clear()
            request.headers[HEADER_APP_KEY] = app.appKey
            requestApply?.invoke(request)
            val response = Client.execute(request)
            val result =
                runCatching { JSONObject(response.body) }.getOrNull()?.optJSONObject("response")
            completion?.invoke(result)
        } catch (e: Exception) {
            Logger.e(LOG_TAG, "Failed to get definitions.", e)
        }
    }
}
