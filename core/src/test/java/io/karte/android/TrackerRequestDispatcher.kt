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
package io.karte.android

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONArray
import org.json.JSONObject

open class TrackerRequestDispatcher : Dispatcher() {
    private val recordedRequests = mutableListOf<RecordedRequest>()

    final override fun dispatch(request: RecordedRequest): MockResponse {
        recordedRequests.add(request)

        request.path?.let {
            if (it.contains("/track") || it.contains("/ingest")) {
                return onTrackRequest(request)
            } else if (it.contains("/overlay")) {
                return MockResponse().setBody("<html></html>")
            } else if (it.contains("/auto-track")) {
                return MockResponse()
            }
        }
        throw IllegalArgumentException("Unexpected request is coming to server.")
    }

    open fun onTrackRequest(request: RecordedRequest): MockResponse {
        return MockResponse().setBody(
            JSONObject().put(
                "response",
                JSONObject().put("messages", JSONArray())
            ).toString()
        )
    }

    fun autoTrackRequests(): List<RecordedRequest> {
        return recordedRequests.filter { it.path?.contains("/auto-track") == true }
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
