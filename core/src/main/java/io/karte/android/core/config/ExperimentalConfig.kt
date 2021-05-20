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
package io.karte.android.core.config

import io.karte.android.core.library.LibraryConfig

/**
 * SDKの設定を保持するクラスです。
 *
 * - Kotlinでは[ExperimentalConfig.build]関数でインスタンスを生成します。
 * - Javaでは[ExperimentalConfig.Builder.build]関数でインスタンスを生成します。
 *
 * @property[operationMode] 動作モードの取得・設定を行います。
 * デフォルトは [OperationMode.DEFAULT] です。
 *
 * **実験的なオプションであるため、通常のSDK利用においてこちらのプロパティを変更する必要はありません。**
 */
class ExperimentalConfig private constructor(
    val operationMode: OperationMode,
    appKey: String,
    baseUrl: String,
    logCollectionUrl: String,
    isDryRun: Boolean,
    isOptOut: Boolean,
    enabledTrackingAaid: Boolean,
    libraryConfigs: List<LibraryConfig>
) : Config(
    appKey,
    baseUrl,
    logCollectionUrl,
    isDryRun,
    isOptOut,
    enabledTrackingAaid,
    libraryConfigs
) {

    /** [ExperimentalConfig]クラスの生成を行うためのクラスです。 */
    class Builder : Config.Builder() {
        /**[ExperimentalConfig.operationMode]を変更します。*/
        var operationMode: OperationMode = OperationMode.DEFAULT @JvmSynthetic set

        /**[ExperimentalConfig.operationMode]を変更します。*/
        fun operationMode(operationMode: OperationMode): Builder =
            apply { this.operationMode = operationMode }

        /**[ExperimentalConfig]クラスのインスタンスを生成します。*/
        override fun build(): ExperimentalConfig = ExperimentalConfig(
            operationMode,
            appKey,
            baseUrl,
            logCollectionUrl,
            isDryRun,
            isOptOut,
            enabledTrackingAaid,
            libraryConfigs
        )
    }

    companion object {
        /**
         * [ExperimentalConfig]クラスのインスタンスを生成します。
         * @param[f] ExperimentalConfigクラスの値を変更するスコープ関数です。
         */
        fun build(f: (Builder.() -> Unit)? = null): ExperimentalConfig {
            val builder = Builder()
            f?.let { builder.it() }
            return builder.build()
        }
    }
}
