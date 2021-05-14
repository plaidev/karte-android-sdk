//
//  Copyright 2021 PLAID, Inc.
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

package io.karte.android.modules.crashreporting

import io.karte.android.core.library.LibraryConfig

/**
 * CrashReportingの設定を保持するクラスです。
 *
 * @property enabledTracking アプリケーションのクラッシュイベント (`native_app_crashed`) の自動送信の有無の取得・設定を行います。
 *
 * `true` の場合はクラッシュイベントの自動送信が有効となり、`false` の場合は無効となります。デフォルトは `true` です。
 */
class CrashReportingConfig private constructor(val enabledTracking: Boolean) : LibraryConfig {

    /**
     * [CrashReportingConfig]クラスの生成を行うためのクラスです。
     */
    class Builder {

        /**[CrashReportingConfig.enabledTracking]を変更します。*/
        var enabledTracking: Boolean = true @JvmSynthetic set

        /**[CrashReportingConfig.enabledTracking]を変更します。*/
        fun enabledTracking(enabledTracking: Boolean): Builder =
            apply { this.enabledTracking = enabledTracking }

        /**[CrashReportingConfig]クラスのインスタンスを生成します。*/
        fun build(): CrashReportingConfig = CrashReportingConfig(enabledTracking)
    }

    companion object {
        /**
         * [CrashReportingConfig]クラスのインスタンスを生成します。
         * @param[f] CrashReportingConfigクラスの値を変更するスコープ関数です。
         */
        fun build(f: (Builder.() -> Unit)? = null): CrashReportingConfig {
            val builder = Builder()
            f?.let { builder.it() }
            return builder.build()
        }
    }
}
