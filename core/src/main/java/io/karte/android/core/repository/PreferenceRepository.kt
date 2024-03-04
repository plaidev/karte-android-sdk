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
package io.karte.android.core.repository

import android.content.Context

private const val PREF_NAME_PREFIX = "io.karte.android.tracker.Data_"

internal class PreferenceRepository internal constructor(
    context: Context,
    appKey: String,
    namespace: String = ""
) : Repository {

    private val prefs =
        context.getSharedPreferences("$PREF_NAME_PREFIX$namespace$appKey", Context.MODE_PRIVATE)

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: String, default: T): T {
        return when (default) {
            is String? -> prefs.getString(key, default) as T
            is Int -> prefs.getInt(key, default) as T
            is Long -> prefs.getLong(key, default) as T
            is Float -> prefs.getFloat(key, default) as T
            is Double -> prefs.getFloat(key, default.toFloat()) as T
            is Boolean -> prefs.getBoolean(key, default) as T
            else -> default
        }
    }

    override fun <T> put(key: String, value: T) {
        val edit = prefs.edit()
        when (value) {
            is String? -> edit.putString(key, value)
            is Int -> edit.putInt(key, value)
            is Long -> edit.putLong(key, value)
            is Float -> edit.putFloat(key, value)
            is Double -> edit.putFloat(key, value.toFloat())
            is Boolean -> edit.putBoolean(key, value)
        }
        edit.apply()
    }

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    override fun removeAll() {
        prefs.edit().clear().apply()
    }

    override fun getAllKeys(): List<String> = prefs.all.keys.toList()
}
