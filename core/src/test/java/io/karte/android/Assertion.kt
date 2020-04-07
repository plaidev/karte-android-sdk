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

import com.google.common.truth.Fact.simpleFact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertAbout
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONArray
import org.json.JSONObject

fun assertThat(jsonArray: JSONArray?): JSONArraySubject {
    return JSONArraySubject.assertThat(jsonArray)
}

fun assertThat(jsonObject: JSONObject?): JSONObjectSubject {
    return JSONObjectSubject.assertThat(jsonObject)
}

class JSONArraySubject private constructor(
    failureMetadata: FailureMetadata,
    private val actual: JSONArray?
) : Subject(failureMetadata, actual) {

    override fun isEqualTo(expected: Any?) {
        if (expected == null || actual == null) {
            if (actual != expected) {
                failWithActual(simpleFact("Expected $actual to be $expected but was not."))
            }
        }
        if (expected.toString() != actual.toString()) {
            failWithActual(simpleFact("Expected $actual to be $expected but was not."))
        }
    }

    companion object {

        // User-defined entry point
        fun assertThat(jsonArray: JSONArray?): JSONArraySubject {
            return assertAbout(JSONARRAY_SUBJECT_FACTORY).that(jsonArray)
        }

        // Static method for getting the subject factory (for use with assertAbout())
        fun jsonArray(): Factory<JSONArraySubject, JSONArray> {
            return JSONARRAY_SUBJECT_FACTORY
        }

        private val JSONARRAY_SUBJECT_FACTORY =
            Factory<JSONArraySubject, JSONArray> { failureMetadata, subject ->
                JSONArraySubject(
                    failureMetadata,
                    subject
                )
            }
    }
}

class JSONObjectSubject private constructor(
    failureMetadata: FailureMetadata,
    private val actual: JSONObject?
) : Subject(failureMetadata, actual) {

    override fun isEqualTo(expected: Any?) {
        if (expected == null || actual == null) {
            if (actual != expected) {
                failWithActual(simpleFact("Expected $actual to be $expected but was not."))
            }
        }
        if (expected.toString() != actual.toString()) {
            failWithActual(simpleFact("Expected $actual to be $expected but was not."))
        }
    }

    companion object {

        // User-defined entry point
        fun assertThat(jsonObject: JSONObject?): JSONObjectSubject {
            return assertAbout(JSONOBJECT_SUBJECT_FACTORY).that(jsonObject)
        }

        // Static method for getting the subject factory (for use with assertAbout())
        fun jsonObject(): Factory<JSONObjectSubject, JSONObject> {
            return JSONOBJECT_SUBJECT_FACTORY
        }

        private val JSONOBJECT_SUBJECT_FACTORY =
            Factory<JSONObjectSubject, JSONObject> { failureMetadata, subject ->
                JSONObjectSubject(
                    failureMetadata,
                    subject
                )
            }
    }
}

fun MockWebServer.assertThatNoEventOccured() {
    // TODO: より直感的な方法で検証したい
    proceedBufferedCall()
    Truth.assertThat(requestCount).isEqualTo(0)
}
