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
package io.karte.android.inappmessaging.internal.javascript

import org.json.JSONException
import org.json.JSONObject

private const val CALLBACK_SCHEME = "karte-tracker-callback://"

internal const val EVENT = "event"
internal const val STATE_CHANGE = "state_changed"
internal const val OPEN_URL = "open_url"
internal const val VISIBILITY = "visibility"
internal const val DOCUMENT_CHANGED = "document_changed"

internal data class Callback(
    val callbackName: String,
    val data: JSONObject
) {
    companion object {

        @Throws(JSONException::class)
        fun parse(name: String, data: String): Callback {
            return Callback(name, JSONObject(data))
        }

        fun isTrackerJsCallback(url: String): Boolean {
            return url.startsWith(CALLBACK_SCHEME)
        }
    }
}
