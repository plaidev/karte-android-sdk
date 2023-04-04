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
package io.karte.android.test_lib

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONArray
import org.json.JSONObject

open class TrackerRequestDispatcher(
    private val onTrack: ((RecordedRequest) -> MockResponse?)? = null
) : Dispatcher() {
    protected val recordedRequests = mutableListOf<RecordedRequest>()

    final override fun dispatch(request: RecordedRequest): MockResponse {
        recordedRequests.add(request)

        request.path?.let { path ->
            onRequest(path, request)?.let { return it }

            if (path.contains("/track") || path.contains("/ingest")) {
                return onTrackRequest(request)
            }
        }
        throw IllegalArgumentException("Unexpected request is coming to server.")
    }

    open fun onRequest(path: String, request: RecordedRequest): MockResponse? {
        return null
    }

    open fun onTrackRequest(request: RecordedRequest): MockResponse {
        onTrack?.invoke(request)?.let { return it }
        return MockResponse().setBody(
            JSONObject().put(
                "response",
                JSONObject().put("messages", JSONArray())
            ).toString()
        )
    }

    fun trackedRequests(): List<RecordedRequest> {
        return recordedRequests.filter { it.path?.contains("/track") == true }
    }

    fun ingestRequests(): List<RecordedRequest> {
        return recordedRequests.filter { it.path?.contains("/ingest") == true }
    }

    fun trackedEvents(): List<JSONObject> {
        return trackedRequests().map { JSONObject(it.parseBody()) }
            .flatMap { it.getJSONArray("events").toList() }
    }

    fun clearHistory() {
        recordedRequests.clear()
    }
}
