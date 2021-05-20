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

import android.content.Context
import io.karte.android.BuildConfig
import io.karte.android.KarteException
import io.karte.android.R
import io.karte.android.core.library.LibraryConfig

/**
 * SDKの設定を保持するクラスです。
 *
 * - Kotlinでは[Config.build]関数でインスタンスを生成します。
 * - Javaでは[Config.Builder.build]関数でインスタンスを生成します。
 *
 * @property[baseUrl] ベースURLの取得・設定を行います。
 *
 * **SDK内部で利用するプロパティであり、通常のSDK利用でこちらのプロパティを利用することはありません。**
 *
 * @property[logCollectionUrl] ログ収集URLの取得・設定を行います。
 *
 * **SDK内部で利用するプロパティであり、通常のSDK利用でこちらのプロパティを利用することはありません。**
 *
 * @property[isDryRun] ドライランの利用有無の取得・設定を行います。
 *
 * ドライランを有効にした場合、[Tracker.track][io.karte.android.tracking.Tracker.track] 等のメソッドを呼び出してもイベントの送信が行われなくなります。
 *
 * `true` の場合はドライランが有効となり、`false` の場合は無効となります。デフォルトは `false` です。
 *
 * @property[isOptOut] オプトアウトの利用有無の取得・設定を行います。
 *
 * なお本設定を有効とした場合であっても、明示的に [KarteApp.optIn][io.karte.android.KarteApp.optIn] を呼び出した場合はオプトイン状態で動作します。
 * 本設定はあくまでも、オプトインまたはオプトアウトの表明を行っていない状態での動作設定を決めるものになります。
 *
 * `true` の場合はデフォルトでオプトアウトが有効となり、`false` の場合は無効となります。デフォルトは `false` です。
 *
 * @property[enabledTrackingAaid] AAID取得の利用有無の取得・設定を行います。
 *
 * `true` の場合はAAID取得が有効となり、`false` の場合は無効となります。デフォルトは `false` です。
 *
 * @property[libraryConfigs] ライブラリの設定の取得・設定を行います。
 *
 * デフォルトは空配列です。
 */
open class Config protected constructor(
    appKey: String,
    val baseUrl: String,
    internal val logCollectionUrl: String,
    val isDryRun: Boolean,
    val isOptOut: Boolean,
    val enabledTrackingAaid: Boolean,
    val libraryConfigs: List<LibraryConfig>
) {
    /**
     * @property[appKey] アプリケーションキーの取得・設定を行います。
     *
     * 設定ファイルから自動でロードされるアプリケーションキー以外を利用したい場合にのみ設定します。
     */
    var appKey: String = appKey
        internal set

    internal val isValidAppKey get() = appKey.length == 32

    /** [Config]クラスの生成を行うためのクラスです。 */
    open class Builder {
        /**[Config.appKey]を変更します。*/
        var appKey: String = "" @JvmSynthetic set

        /**[Config.baseUrl]を変更します。*/
        var baseUrl: String = "https://api.karte.io/v0/native" @JvmSynthetic set

        /**[Config.logCollectionUrl]を変更します。*/
        internal var logCollectionUrl: String =
            "https://us-central1-production-debug-log-collector.cloudfunctions.net/nativeAppLogUrl"
            @JvmSynthetic set

        /**[Config.isDryRun]を変更します。*/
        var isDryRun: Boolean = false @JvmSynthetic set

        /**[Config.isOptOut]を変更します。*/
        var isOptOut: Boolean = false @JvmSynthetic set

        /**[Config.enabledTrackingAaid]を変更します。*/
        var enabledTrackingAaid: Boolean = false @JvmSynthetic set

        /**[Config.libraryConfigs]を変更します。*/
        var libraryConfigs: List<LibraryConfig> = listOf() @JvmSynthetic set

        /**[Config.baseUrl]を変更します。*/
        fun baseUrl(baseUrl: String): Builder = apply { this.baseUrl = baseUrl }

        /**[Config.logCollectionUrl]を変更します。*/
        internal fun logCollectionUrl(logCollectionUrl: String): Builder =
            apply { this.logCollectionUrl = logCollectionUrl }

        /**[Config.isDryRun]を変更します。*/
        fun isDryRun(isDryRun: Boolean): Builder = apply { this.isDryRun = isDryRun }

        /**[Config.isOptOut]を変更します。*/
        fun isOptOut(isOptOut: Boolean): Builder = apply { this.isOptOut = isOptOut }

        /**[Config.enabledTrackingAaid]を変更します。*/
        fun enabledTrackingAaid(enabledTrackingAaid: Boolean): Builder =
            apply { this.enabledTrackingAaid = enabledTrackingAaid }

        /**[Config.libraryConfigs]を変更します。*/
        fun libraryConfigs(libraryConfigs: List<LibraryConfig>): Builder =
            apply { this.libraryConfigs = libraryConfigs }

        /**[Config.libraryConfigs]を変更します。*/
        fun libraryConfigs(vararg libraryConfigs: LibraryConfig): Builder =
            apply { this.libraryConfigs = libraryConfigs.toList() }

        /**[Config]クラスのインスタンスを生成します。*/
        open fun build(): Config = Config(
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
         * [Config]クラスのインスタンスを生成します。
         * @param[f] Configクラスの値を変更するスコープ関数です。
         */
        fun build(f: (Builder.() -> Unit)? = null): Config {
            val builder = Builder()
            f?.let { builder.it() }
            return builder.build()
        }

        internal fun withAppKey(context: Context, config: Config?): Config {
            if (config != null && config.appKey.isNotEmpty()) return config
            return (config ?: build()).apply { this.appKey = appKeyFromResource(context) }
        }

        private fun appKeyFromResource(context: Context): String {
            val res = context.resources
            val pkg = res.getResourcePackageName(R.id.karte_resources)
            val id = res.getIdentifier("karte_app_key", "string", pkg)
            return if (id == 0) {
                if (BuildConfig.DEBUG) {
                    throw KarteException("karte_resources.xml not found.")
                } else {
                    ""
                }
            } else {
                res.getString(id)
            }
        }
    }
}
