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
