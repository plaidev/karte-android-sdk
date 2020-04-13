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
package io.karte.android.tracking

/**
 * トラッキング処理過程の一部を委譲するためのタイプ。
 */
interface TrackerDelegate {
    /**
     * イベントに紐付けるカスタムオブジェクトの生成処理に割り込みます。
     *
     * 編集済みのイベントを返すことで、イベントの内容を編集することが可能です。
     *
     * @param event イベント
     * @return 編集済みのイベントを返します
     */
    fun intercept(event: Event): Event
}
