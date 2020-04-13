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
package io.karte.android.inappmessaging

import android.net.Uri

/**
 * アプリ内メッセージで発生するイベントを委譲するためのクラスです。
 */
abstract class InAppMessagingDelegate {
    /**
     * アプリ内メッセージ用のWindowが表示されたことを通知します。
     */
    open fun onWindowPresented() {}

    /**
     * アプリ内メッセージ用のWindowが非表示になったことを通知します。
     */
    open fun onWindowDismissed() {}

    /**
     * 接客サービスアクションが表示されたことを通知します。
     *
     * @param[campaignId] 接客サービスのキャンペーンID
     * @param[shortenId] 接客サービスアクションの短縮ID
     */
    open fun onPresented(campaignId: String, shortenId: String) {}

    /**
     * 接客サービスアクションが非表示になったことを通知します。
     *
     * @param[campaignId] 接客サービスのキャンペーンID
     * @param[shortenId] 接客サービスアクションの短縮ID
     */
    open fun onDismissed(campaignId: String, shortenId: String) {}

    /**
     * 接客サービスアクション中のボタンがクリックされた際に、リンクをSDK側で自動的に処理するかどうか問い合わせます。
     *
     * @param[url] リンクURL
     * @return `true` を返した場合はSDK側でリンクを自動で開きます。`false` を返した場合はSDK側では何もしません。
     */
    open fun shouldOpenURL(url: Uri): Boolean {
        return true
    }
}
