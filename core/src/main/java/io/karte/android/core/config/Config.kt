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
import android.net.Uri
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
 * @param[baseUrl] ベースURLの取得・設定を行います。
 * URLを変更することで、地域や環境を設定することができます。
 *
 * @property[logCollectionUrl] ログ収集URLの取得・設定を行います。
 * ログ収集機能は廃止されたため、この設定は使用されません。
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
    apiKey: String,
    baseUrl: String,
    @Deprecated("No longer used")
    internal val logCollectionUrl: String,
    val isDryRun: Boolean,
    val isOptOut: Boolean,
    val enabledTrackingAaid: Boolean,
    val libraryConfigs: List<LibraryConfig>
) {
    /** アプリケーションキーの取得を行います。 */
    var appKey: String = appKey
        internal set

    internal val isValidAppKey get() = appKey.length == 32

    /** APIキーの取得を行います。 */
    var apiKey: String = apiKey
        private set

    /** 未設定確認用変数 */
    private var _baseUrl: String = ""

    /**
     * ベースURLの取得を行います。
     * 設定されたURLにサブパスを付与したものを返します。
     */
    var baseUrl: String
        private set(value) {
            if (value.isEmpty())
                return
            _baseUrl = Uri.withAppendedPath(Uri.parse(value), "v0/native").toString()
        }
        get() {
            if (_baseUrl.isEmpty())
                return "https://b.karte.io/v0/native"
            return _baseUrl
        }

    private var _dataLocation: String = ""
    /** KARTEプロジェクトのデータロケーションを取得します。 */
    var dataLocation: String
        private set(value) {
            _dataLocation = value
        }
        get() {
            if (_dataLocation.isEmpty())
                return "tw"
            return _dataLocation
        }

    init {
        this.baseUrl = baseUrl
    }

    /** [Config]クラスの生成を行うためのクラスです。 */
    open class Builder {
        /**
         * [Config.appKey]を変更します。
         * 設定ファイルから自動でロードされる値以外を利用したい場合にのみ設定します。
         */
        var appKey: String = "" @JvmSynthetic set

        /**
         * [Config.apiKey]を変更します。
         * 設定ファイルから自動でロードされる値以外を利用したい場合にのみ設定します。
         */
        var apiKey: String = "" @JvmSynthetic set

        /**
         * [Config.baseUrl]を変更します。
         * URLを変更することで、地域や環境を設定することができます。
         *
         * 設定ファイルから自動でロードされる値以外を利用したい場合にのみ設定します。
         */
        var baseUrl: String = "" @JvmSynthetic set

        /**
         * [Config.dataLocation]を変更します。
         * 設定ファイルから自動でロードされる値以外を利用したい場合にのみ設定します。
         */
        var dataLocation: String = "" @JvmSynthetic set

        /**[Config.isDryRun]を変更します。*/
        var isDryRun: Boolean = false @JvmSynthetic set

        /**[Config.isOptOut]を変更します。*/
        var isOptOut: Boolean = false @JvmSynthetic set

        /**[Config.enabledTrackingAaid]を変更します。*/
        var enabledTrackingAaid: Boolean = false @JvmSynthetic set

        /**[Config.libraryConfigs]を変更します。*/
        var libraryConfigs: List<LibraryConfig> = listOf() @JvmSynthetic set

        /**
         * [Config.appKey]を変更します。
         * 設定ファイルから自動でロードされる値以外を利用したい場合にのみ設定します。
         */
        fun appKey(appKey: String): Builder = apply { this.appKey = appKey }

        /**
         * [Config.apiKey]を変更します。
         * 設定ファイルから自動でロードされる値以外を利用したい場合にのみ設定します。
         */
        fun apiKey(apiKey: String): Builder = apply { this.apiKey = apiKey }

        /**
         * [Config.baseUrl]を変更します。
         * URLを変更することで、地域や環境を設定することができます。
         *
         * 設定ファイルから自動でロードされる値以外を利用したい場合にのみ設定します。
         */
        fun baseUrl(baseUrl: String): Builder = apply { this.baseUrl = baseUrl }

        /**
         * [Config.dataLocation]を変更します。
         * 設定ファイルから自動でロードされる値以外を利用したい場合にのみ設定します。
         */
        fun dataLocation(dataLocation: String): Builder = apply { this.dataLocation = dataLocation }

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
            apiKey,
            baseUrl,
            "",
            isDryRun,
            isOptOut,
            enabledTrackingAaid,
            libraryConfigs
        ).also { config ->
            config.dataLocation = dataLocation
        }
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

        /**
         * 初期化済みのconfigのパラメータをresourceから補完する
         *
         * * configがnullならデフォルト値で初期化する
         * * パラメータに空文字以外が設定済みなら上書きしない
         * * 設定ファイルに存在しなければ補完しない
         * * **app_keyがここまでで与えられなかった場合、デバッグビルドでは例外をスローする**
         */
        internal fun fillFromResource(context: Context, config: Config?): Config {
            val cfg = config ?: build()
            if (cfg.appKey.isEmpty()) {
                val appKeyFromResource = readStringFromResource(context, "karte_app_key")
                // appKeyに限り 未設定＆resourceにない＆debugビルド 時に例外スロー
                if (appKeyFromResource == null && BuildConfig.DEBUG)
                    throw KarteException("karte_resources.xml not found.")
                appKeyFromResource?.let { cfg.appKey = it }
            }
            if (cfg.apiKey.isEmpty()) {
                readStringFromResource(context, "karte_api_key")?.let {
                    cfg.apiKey = it
                }
            }
            if (cfg._baseUrl.isEmpty()) {
                readStringFromResource(context, "karte_base_url")?.let {
                    cfg.baseUrl = it
                }
            }
            if (cfg._dataLocation.isEmpty()) {
                readStringFromResource(context, "karte_data_location")?.let {
                    cfg.dataLocation = it
                }
            }
            return cfg
        }

        private fun readStringFromResource(context: Context, name: String): String? {
            val res = context.resources
            val pkg = res.getResourcePackageName(R.id.karte_resources)
            val id = res.getIdentifier(name, "string", pkg)
            return if (id == 0) {
                null
            } else {
                res.getString(id)
            }
        }
    }
}
