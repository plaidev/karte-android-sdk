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
package io.karte.android.visualtracking.integration

import io.karte.android.TrackerRequestDispatcher
import io.karte.android.visualtracking.autoTrackDefinition
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONObject

class VTRequestDispatcher(vararg definitions: JSONObject, private val lastModified: Long = 0) :
    TrackerRequestDispatcher() {
    private val autoTrackDefinition = autoTrackDefinition(*definitions, lastModified = lastModified)

    override fun onRequest(path: String, request: RecordedRequest): MockResponse? {
        if (path.contains("/auto-track/definitions")) {
            val response = if (isModified(request)) autoTrackDefinition else JSONObject()
            return MockResponse().setBody(JSONObject().put("response", response).toString())
        }
        if (path.contains("/auto-track"))
            return MockResponse()
        return super.onRequest(path, request)
    }

    override fun onTrackRequest(request: RecordedRequest): MockResponse {
        if (isModified(request))
            return MockResponse().setBody(
                JSONObject()
                    .put(
                        "response", JSONObject()
                            .put("auto_track_definition", autoTrackDefinition)
                    ).toString()
            )
        return super.onTrackRequest(request)
    }

    fun autoTrackRequests(): List<RecordedRequest> {
        return recordedRequests.filter { it.path?.contains("/auto-track") == true }
    }

    private fun isModified(request: RecordedRequest): Boolean {
        if (request.getHeader("X-KARTE-Auto-Track-OS") == "android") {
            val since = request.getHeader("X-KARTE-Auto-Track-If-Modified-Since")
            if (since != null && Integer.parseInt(since) <= lastModified) {
                return false
            }
            return true
        }
        return false
    }
}
