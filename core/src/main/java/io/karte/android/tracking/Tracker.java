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
package io.karte.android.tracking;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.util.Map;

/**
 * イベントトラッキングを行うためのクラスです。
 * 送信するイベントの種類に応じて、複数のメソッドが用意されております。
 *
 * <h3>track</h3>
 * 任意の名前のイベントを送る場合に利用します。
 *
 * <h3>identify</h3>
 * ユーザーに関する情報（ユーザーIDや名前、メールアドレス等）を送る場合に利用します。
 *
 * <h3>view</h3>
 * 画面表示に関する情報を送る場合に利用します。<br>
 * 通常は {@link android.app.Activity#onCreate(Bundle) onCreate} 等で呼び出します。
 * <p>
 * なおViewイベントに関しては、イベントの送信だけではなくアプリ上で画面遷移が発生したことを認識するためのものとしても利用されます。<br>
 * 具体的には、Viewイベントを発火させたタイミングで、既にアプリ内メッセージが表示されている場合は、自動でアプリ内メッセージを非表示にします。<br>
 * また <a href="https://support.karte.io/post/3JaA3BlXQea59AaPGxD3bb">ネイティブアプリにおける接客表示制限</a> オプションを有効にした場合にも、ここで設定した認識結果が利用されます。
 */
public final class Tracker {
    private Tracker() {
    }

    /**
     * イベントの送信を行います。
     *
     * @param event {@link io.karte.android.tracking.Event} オブジェクト
     */
    public static void track(@NonNull Event event) {
        TrackingService.track(event, null, null);
    }

    /**
     * イベントの送信を行います。
     *
     * @param event     {@link io.karte.android.tracking.Event} オブジェクト
     * @param visitorId visitorId
     */
    public static void track(@NonNull Event event, @Nullable String visitorId) {
        TrackingService.track(event, visitorId, null);
    }

    /**
     * イベントの送信を行います。
     *
     * @param event      {@link io.karte.android.tracking.Event} オブジェクト
     * @param completion 処理の完了を受け取るInterface
     */
    public static void track(@NonNull Event event, @Nullable TrackCompletion completion) {
        TrackingService.track(event, null, completion);
    }

    /**
     * イベントの送信を行います。
     *
     * @param event      {@link io.karte.android.tracking.Event} オブジェクト
     * @param visitorId  visitorId
     * @param completion 処理の完了を受け取るInterface
     */
    public static void track(@NonNull Event event, @Nullable String visitorId, @Nullable TrackCompletion completion) {
        TrackingService.track(event, visitorId, completion);
    }

    /**
     * イベントの送信を行います。
     *
     * @param name イベント名
     */
    public static void track(@NonNull String name) {
        TrackingService.track(name, (Map<String, ?>) null, null);
    }

    /**
     * イベントの送信を行います。
     *
     * @param name       イベント名
     * @param completion 処理の完了を受け取るInterface
     */
    public static void track(@NonNull String name, @Nullable TrackCompletion completion) {
        TrackingService.track(name, (Map<String, ?>) null, completion);
    }

    /**
     * イベントの送信を行います。
     *
     * @param name   イベント名
     * @param values イベントに紐付けるカスタムオブジェクト
     */
    public static void track(@NonNull String name, @Nullable Map<String, ?> values) {
        TrackingService.track(name, values, null);
    }

    /**
     * イベントの送信を行います。
     *
     * @param name       イベント名
     * @param values     イベントに紐付けるカスタムオブジェクト
     * @param completion 処理の完了を受け取るInterface
     */
    public static void track(@NonNull String name, @Nullable Map<String, ?> values, @Nullable TrackCompletion completion) {
        TrackingService.track(name, values, completion);
    }

    /**
     * イベントの送信を行います。
     *
     * @param name       イベント名
     * @param jsonObject イベントに紐付けるカスタムオブジェクト
     */
    public static void track(@NonNull String name, @Nullable JSONObject jsonObject) {
        TrackingService.track(name, jsonObject, null);
    }

    /**
     * イベントの送信を行います。
     *
     * @param name       イベント名
     * @param jsonObject イベントに紐付けるカスタムオブジェクト
     * @param completion 処理の完了を受け取るInterface
     */
    public static void track(@NonNull String name, @Nullable JSONObject jsonObject, @Nullable TrackCompletion completion) {
        TrackingService.track(name, jsonObject, completion);
    }

    /**
     * Identifyイベントの送信を行います。
     *
     * @param values Identifyイベントに紐付けるカスタムオブジェクト
     */
    public static void identify(@NonNull Map<String, ?> values) {
        TrackingService.identify(values, null);
    }

    /**
     * Identifyイベントの送信を行います。
     *
     * @param values     Identifyイベントに紐付けるカスタムオブジェクト
     * @param completion 処理の完了を受け取るInterface
     */
    public static void identify(@NonNull Map<String, ?> values, @Nullable TrackCompletion completion) {
        TrackingService.identify(values, completion);
    }

    /**
     * Identifyイベントの送信を行います。
     *
     * @param jsonObject Identifyイベントに紐付けるカスタムオブジェクト
     */
    public static void identify(@NonNull JSONObject jsonObject) {
        TrackingService.identify(jsonObject, null);
    }

    /**
     * Identifyイベントの送信を行います。
     *
     * @param jsonObject Identifyイベントに紐付けるカスタムオブジェクト
     * @param completion 処理の完了を受け取るInterface
     */
    public static void identify(@NonNull JSONObject jsonObject, @Nullable TrackCompletion completion) {
        TrackingService.identify(jsonObject, completion);
    }

    /**
     * Viewイベントの送信を行います。
     *
     * @param viewName 画面名
     */
    public static void view(@NonNull String viewName) {
        TrackingService.view(viewName, null, (Map<String, ?>) null, null);
    }

    /**
     * Viewイベントの送信を行います。
     *
     * @param viewName 画面名
     * @param title    タイトル
     */
    public static void view(@NonNull String viewName, @Nullable String title) {
        TrackingService.view(viewName, title, (Map<String, ?>) null, null);
    }

    /**
     * Viewイベントの送信を行います。
     *
     * @param viewName 画面名
     * @param title    タイトル
     * @param values   Viewイベントに紐付けるカスタムオブジェクト
     */
    public static void view(@NonNull String viewName, @Nullable String title, @Nullable Map<String, ?> values) {
        TrackingService.view(viewName, title, values, null);
    }

    /**
     * Viewイベントの送信を行います。
     *
     * @param viewName   画面名
     * @param title      タイトル
     * @param values     Viewイベントに紐付けるカスタムオブジェクト
     * @param completion 処理の完了を受け取るInterface
     */
    public static void view(@NonNull String viewName, @Nullable String title, @Nullable Map<String, ?> values, @Nullable TrackCompletion completion) {
        TrackingService.view(viewName, title, values, completion);
    }

    /**
     * Viewイベントの送信を行います。
     *
     * @param viewName   画面名
     * @param title      タイトル
     * @param jsonObject Viewイベントに紐付けるカスタムオブジェクト
     */
    public static void view(@NonNull String viewName, @Nullable String title, @Nullable JSONObject jsonObject) {
        TrackingService.view(viewName, title, jsonObject, null);
    }

    /**
     * Viewイベントの送信を行います。
     *
     * @param viewName   画面名
     * @param title      タイトル
     * @param jsonObject Viewイベントに紐付けるカスタムオブジェクト
     * @param completion 処理の完了を受け取るInterface
     */
    public static void view(@NonNull String viewName, @Nullable String title, @Nullable JSONObject jsonObject, @Nullable TrackCompletion completion) {
        TrackingService.view(viewName, title, jsonObject, completion);
    }

    /**
     * Viewイベントの送信を行います。
     *
     * @param viewName 画面名
     * @param values   Viewイベントに紐付けるカスタムオブジェクト
     */
    public static void view(@NonNull String viewName, @Nullable Map<String, ?> values) {
        TrackingService.view(viewName, null, values, null);
    }

    /**
     * Viewイベントの送信を行います。
     *
     * @param viewName   画面名
     * @param values     Viewイベントに紐付けるカスタムオブジェクト
     * @param completion 処理の完了を受け取るInterface
     */
    public static void view(@NonNull String viewName, @Nullable Map<String, ?> values, @Nullable TrackCompletion completion) {
        TrackingService.view(viewName, null, values, completion);
    }

    /**
     * Viewイベントの送信を行います。
     *
     * @param viewName   画面名
     * @param jsonObject Viewイベントに紐付けるカスタムオブジェクト
     */
    public static void view(@NonNull String viewName, @Nullable JSONObject jsonObject) {
        TrackingService.view(viewName, null, jsonObject, null);
    }

    /**
     * Viewイベントの送信を行います。
     *
     * @param viewName   画面名
     * @param jsonObject Viewイベントに紐付けるカスタムオブジェクト
     * @param completion 処理の完了を受け取るInterface
     */
    public static void view(@NonNull String viewName, @Nullable JSONObject jsonObject, @Nullable TrackCompletion completion) {
        TrackingService.view(viewName, null, jsonObject, completion);
    }

    /**
     * Viewイベントの送信を行います。
     *
     * @param viewName   画面名
     * @param completion 処理の完了を受け取るInterface
     */
    public static void view(@NonNull String viewName, @Nullable TrackCompletion completion) {
        TrackingService.view(viewName, null, (Map<String, ?>) null, completion);
    }

    /**
     * トラッカー処理のデリゲートインスタンスを設定します。
     *
     * @param delegate 委譲先インスタンス
     */
    public static void setDelegate(@Nullable TrackerDelegate delegate) {
        TrackingService.setDelegate(delegate);
    }
}
