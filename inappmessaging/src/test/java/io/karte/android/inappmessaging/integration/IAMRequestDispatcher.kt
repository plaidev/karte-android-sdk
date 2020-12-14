package io.karte.android.inappmessaging.integration

import io.karte.android.TrackerRequestDispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

class IAMRequestDispatcher(onTrack: ((RecordedRequest) -> MockResponse)? = null) :
    TrackerRequestDispatcher(onTrack) {
    override fun onRequest(path: String, request: RecordedRequest): MockResponse? {
        if (path.contains("/overlay"))
            return MockResponse().setBody("<html></html>")
        return super.onRequest(path, request)
    }
}
