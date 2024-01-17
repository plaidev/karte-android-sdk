//
//  Copyright 2023 PLAID, Inc.
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

package io.karte.android.inappmessaging

import io.karte.android.core.library.LibraryConfig

private const val OVERLAY_DEFAULT_URL = "https://cf-native.karte.io"

/**
 * InAppMessagingモジュールの設定を保持するクラスです。
 */
class InAppMessagingConfig private constructor(overlayBaseUrl: String) : LibraryConfig {

    private var _overlayBaseUrl: String = ""

    /**
     * overlayBaseUrl overlayベースURLの取得を行います。
     */
    var overlayBaseUrl: String
        private set(value) {
            if (value.isEmpty())
                return
            _overlayBaseUrl = value
        }
        get() {
            if (_overlayBaseUrl.isEmpty())
                return OVERLAY_DEFAULT_URL
            return _overlayBaseUrl
        }

    init {
        this.overlayBaseUrl = overlayBaseUrl
    }

    /**
     * [InAppMessagingConfig]クラスの生成を行うためのクラスです。
     */
    class Builder {

        /**
         * [InAppMessagingConfig.overlayBaseUrl]を変更します。
         * 未指定（空文字）時には、デフォルト値が使用されます。
         *
         * **SDK内部で利用するプロパティであり、通常のSDK利用でこちらのプロパティを利用することはありません。**
         */
        var overlayBaseUrl: String = "" @JvmSynthetic set

        /**
         * [InAppMessagingConfig.overlayBaseUrl]を変更します。
         * 未指定（空文字）時には、デフォルト値が使用されます。
         *
         * **SDK内部で利用するプロパティであり、通常のSDK利用でこちらのプロパティを利用することはありません。**
         */
        fun overlayBaseUrl(overlayBaseUrl: String): Builder =
            apply { this.overlayBaseUrl = overlayBaseUrl }

        /**[InAppMessagingConfig]クラスのインスタンスを生成します。*/
        fun build(): InAppMessagingConfig = InAppMessagingConfig(overlayBaseUrl)
    }

    companion object {
        /**
         * [InAppMessagingConfig]クラスのインスタンスを生成します。
         * @param[f] InAppMessagingConfigクラスの値を変更するスコープ関数です。
         */
        fun build(f: (Builder.() -> Unit)? = null): InAppMessagingConfig {
            val builder = Builder()
            f?.let { builder.it() }
            return builder.build()
        }
    }
}
