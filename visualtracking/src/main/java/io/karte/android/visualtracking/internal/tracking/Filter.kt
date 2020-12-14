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

import org.json.JSONException
import org.json.JSONObject
import java.util.HashMap

internal class Filter private constructor(
    private val pathList: Array<String>,
    private val test: Test
) {
    private abstract class Comparator {
        internal abstract fun compare(value1: Any?, value2: Any?): Boolean
    }

    @Throws(JSONException::class)
    fun filter(value: JSONObject): Boolean {
        var target = value
        val length = pathList.size
        if (length == 0) return false
        for (i in 0 until length - 1) {
            target = target.getJSONObject(pathList[i])
        }
        return test.compare(target.opt(pathList[length - 1]))
    }

    private class Test internal constructor(
        private val comparator: Comparator,
        internal var param: Any
    ) {

        internal fun compare(target: Any?): Boolean {
            return this.comparator.compare(target, param)
        }
    }

    companion object {

        private val EQ_COMPARATOR = object : Comparator() {
            override fun compare(value1: Any?, value2: Any?): Boolean {
                if (value1 == null && value2 == null) return true
                return if (value1 == null) false else value1 == value2
            }
        }

        private val NE_COMPARATOR = object : Comparator() {
            override fun compare(value1: Any?, value2: Any?): Boolean {
                return !EQ_COMPARATOR.compare(value1, value2)
            }
        }

        private val STARTS_WITH_COMPARATOR = object : Comparator() {
            override fun compare(value1: Any?, value2: Any?): Boolean {
                return if (value1 !is String || value2 !is String) false
                else value1.startsWith(value2)
            }
        }

        private val CONTAINS_COMPARATOR = object : Comparator() {
            override fun compare(value1: Any?, value2: Any?): Boolean {
                return if (value1 !is String || value2 !is String) false
                else value1.contains(value2)
            }
        }

        private val ENDS_WITH_COMPARATOR = object : Comparator() {
            override fun compare(value1: Any?, value2: Any?): Boolean {
                return if (value1 !is String || value2 !is String) false
                else value1.endsWith(value2)
            }
        }

        private val op2Comparator = object : HashMap<String, Comparator>() {
            init {
                this["\$eq"] = EQ_COMPARATOR
                this["\$ne"] = NE_COMPARATOR
                this["\$startsWith"] = STARTS_WITH_COMPARATOR
                this["\$contains"] = CONTAINS_COMPARATOR
                this["\$endsWith"] = ENDS_WITH_COMPARATOR
            }
        }

        private fun parsePath(path: String): Array<String> {
            return path.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        }

        @Throws(JSONException::class)
        fun parseQuery(query: JSONObject): Filter {
            val propertyName = query.keys().next()
            return Filter(parsePath(propertyName), parseTest(query.getJSONObject(propertyName)))
        }

        @Throws(JSONException::class)
        private fun parseTest(query: JSONObject): Test {
            val comp = query.keys().next()
            return Test(op2Comparator[comp]!!, query.get(comp))
        }
    }
}
