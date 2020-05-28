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
package io.karte.android.core.logger

import android.util.Log
import io.karte.android.BuildConfig

/**
 * ログを出力するためのクラスです。
 */
object Logger {
    /**
     * ログレベルの取得および設定を行います。
     */
    @JvmStatic
    var level: LogLevel = if (BuildConfig.DEBUG) LogLevel.VERBOSE else LogLevel.WARN

    /**
     * ログ(Verbose)を出力します。
     * @param[tag] タグ
     * @param[message] メッセージ
     * @param[throwable] 例外オブジェクト
     */
    @JvmStatic
    @JvmOverloads
    fun v(tag: String?, message: String, throwable: Throwable? = null) {
        if (level > LogLevel.VERBOSE) return

        if (throwable != null) {
            Log.v(tag, message)
        } else {
            Log.v(tag, message, throwable)
        }
    }

    /**
     * ログ(Debug)を出力します。
     * @param[tag] タグ
     * @param[message] メッセージ
     * @param[throwable] 例外オブジェクト
     */
    @JvmStatic
    @JvmOverloads
    fun d(tag: String?, message: String, throwable: Throwable? = null) {
        if (level > LogLevel.DEBUG) return

        if (throwable != null) {
            Log.d(tag, message)
        } else {
            Log.d(tag, message, throwable)
        }
    }

    /**
     * ログ(Info)を出力します。
     * @param[tag] タグ
     * @param[message] メッセージ
     * @param[throwable] 例外オブジェクト
     */
    @JvmStatic
    @JvmOverloads
    fun i(tag: String?, message: String, throwable: Throwable? = null) {
        if (level > LogLevel.INFO) return

        if (throwable != null) {
            Log.i(tag, message)
        } else {
            Log.i(tag, message, throwable)
        }
    }

    /**
     * ログ(Warning)を出力します。
     * @param[tag] タグ
     * @param[message] メッセージ
     * @param[throwable] 例外オブジェクト
     */
    @JvmStatic
    @JvmOverloads
    fun w(tag: String?, message: String, throwable: Throwable? = null) {
        if (level > LogLevel.WARN) return

        if (throwable != null) {
            Log.w(tag, message)
        } else {
            Log.w(tag, message, throwable)
        }
    }

    /**
     * ログ(Error)を出力します。
     * @param[tag] タグ
     * @param[message] メッセージ
     * @param[throwable] 例外オブジェクト
     */
    @JvmStatic
    @JvmOverloads
    fun e(tag: String?, message: String, throwable: Throwable? = null) {
        if (level > LogLevel.ERROR) return

        if (throwable != null) {
            Log.e(tag, message)
        } else {
            Log.e(tag, message, throwable)
        }
    }
}

/**
 * ログレベルを表す列挙型です。
 */
enum class LogLevel {
    /** VERBOSE */
    VERBOSE,

    /** DEBUG */
    DEBUG,

    /** INFO */
    INFO,

    /** WARN */
    WARN,

    /** ERROR */
    ERROR
}
