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
package io.karte.android.utilities.http

/**
 * HTTP Response Object class.
 * @property[code] HTTP Response code
 * @property[headers] HTTP Response headers
 * @property[body] Response body
 */
open class Response(val code: Int, val headers: Map<String, List<String>>, val body: String) {

    /** is Request successful .  */
    val isSuccessful: Boolean
        get() = code in 200..299

    /** Is server maintenance response.  */
    val isMaintenance: Boolean
        get() = code == 503

    /** @suppress */
    override fun toString(): String {
        return "Response{" +
            "code=" + code +
            ", headers=" + headers +
            ", body='" + body + '\''.toString() +
            '}'.toString()
    }
}
