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
package io.karte.android.utilities

import io.karte.android.tracking.Values
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

fun InputStream.asString(): String {
    val os = ByteArrayOutputStream()
    copyTo(os, 4096)
    return try {
        os.toString("UTF-8")
    } catch (e: UnsupportedEncodingException) {
        os.toString()
    }
}

private val ASCII_REGEX = Pattern.compile("^[\\u0020-\\u007E]+$")
fun String.isAscii(): Boolean {
    if (this.isEmpty()) {
        return false
    }
    val m = ASCII_REGEX.matcher(this)
    return m.find()
}

//region Any
fun Any.getLowerClassName(): String = this::class.java.simpleName.toLowerCase(Locale.ROOT)

private fun Any?.unwrapJson(): Any? {
    return when (this) {
        is JSONObject -> this.toMap()
        is JSONArray -> this.toList()
        null, JSONObject.NULL -> null
        else -> this
    }
}

private fun Any?.format(): Any? {
    return when (this) {
        is JSONObject -> this.format()
        is JSONArray -> this.format()
        is Map<*, *> -> this.mapValues { it.value.format() }.filter { it.value != null }
        is List<*> -> this.mapNotNull { it.format() }
        is Date -> this.time / 1000
        else -> this
    }
}
//endregion

//region JSONArray
fun JSONArray.forEach(operation: (Any) -> Unit) {
    repeat(length()) { index ->
        operation(get(index))
    }
}

fun JSONArray.map(transform: (Any?) -> Any?): List<Any?> {
    val list = mutableListOf<Any?>()
    forEach { list.add(transform(it)) }
    return list
}

fun JSONArray.toList(): List<Any?> {
    return map { it.unwrapJson() }
}

fun JSONArray.format(): JSONArray {
    val array = JSONArray()
    forEach { value -> value.format()?.let { array.put(it) } }
    return array
}
//endregion

//region JSONObject
fun JSONObject.forEach(action: (String, Any?) -> Unit) {
    keys().forEach { key -> opt(key)?.let { value -> action(key, value) } }
}

fun JSONObject.toMap(): Map<String, Any?> {
    val map = mutableMapOf<String, Any?>()
    forEach { key, value ->
        map[key] = value.unwrapJson()
    }
    return map
}

fun JSONObject.format(): JSONObject {
    val obj = JSONObject()
    forEach { key, value ->
        value.format()?.let { obj.put(key, it) }
    }
    return obj
}

fun JSONObject.toValues(): Values {
    return toMap().filterValues { it != null } as Values
}
//endregion

fun Values.format(): Values {
    return mapValues { it.value.format() }.filterValues { it != null } as Values
}
