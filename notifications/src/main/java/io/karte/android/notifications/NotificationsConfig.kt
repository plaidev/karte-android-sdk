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

package io.karte.android.notifications

import io.karte.android.core.library.LibraryConfig

/**
 * Notificationsモジュールの設定を保持するクラスです。
 *
 * @property enabledFCMTokenResend FCMTokenの自動送信の有無の取得・設定を行います。
 *
 * `true` の場合はFCMTokenの自動送信が有効となり、`false` の場合は無効となります。デフォルトは `true` です。
 */
class NotificationsConfig private constructor(val enabledFCMTokenResend: Boolean) : LibraryConfig {

    /**
     * [NotificationsConfig]クラスの生成を行うためのクラスです。
     */
    class Builder {

        /**[NotificationsConfig.enabledFCMTokenResend]を変更します。*/
        var enabledFCMTokenResend: Boolean = true @JvmSynthetic set

        /**[NotificationsConfig.enabledFCMTokenResend]を変更します。*/
        fun enabledFCMTokenResend(enabledFCMTokenResend: Boolean): Builder =
            apply { this.enabledFCMTokenResend = enabledFCMTokenResend }

        /**[NotificationsConfig]クラスのインスタンスを生成します。*/
        fun build(): NotificationsConfig = NotificationsConfig(enabledFCMTokenResend)
    }

    companion object {
        /**
         * [NotificationsConfig]クラスのインスタンスを生成します。
         * @param[f] NotificationsConfigクラスの値を変更するスコープ関数です。
         */
        fun build(f: (Builder.() -> Unit)? = null): NotificationsConfig {
            val builder = Builder()
            f?.let { builder.it() }
            return builder.build()
        }
    }
}
