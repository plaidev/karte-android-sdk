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
package io.karte.android.variables;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

import io.karte.android.variables.internal.VariablesService;


/**
 * 設定値の取得・管理を司るクラスです。
 */
public class Variables {
    private Variables() {
    }

    /**
     * 設定値を取得し、端末上にキャッシュします。
     */
    public static void fetch() {
        VariablesService.fetch();
    }

    /**
     * 設定値を取得し、端末上にキャッシュします。
     *
     * @param completion 取得完了ハンドラ
     */
    public static void fetch(@Nullable FetchCompletion completion) {
        VariablesService.fetch(completion);
    }

    /**
     * 指定されたキーに関連付けられた設定値にアクセスします。
     * なお設定値にアクセスするには事前に {@link Variables#fetch()} を呼び出しておく必要があります。
     *
     * @param key 検索するためのキー
     * @return キーに関連付けられた設定値を返します。
     */
    public static @NotNull
    Variable get(@NotNull String key) {
        return VariablesService.get(key);
    }

    /**
     * 全ての設定値のキーの一覧を取得できます。
     * なお、事前に {@link Variables#fetch()} を呼び出しておく必要があります。
     *
     * @return キーに関連付けられた設定値のキーの一覧を返します。
     */
    public static @NotNull
    List<String> getAllKeys() {
        return VariablesService.getAllKeys();
    }

    /**
     * 指定したキーの設定値のキャッシュが削除されます。
     */
    public static void clearCacheByKey(@NotNull String key) {
        VariablesService.clearCacheByKey(key);
    }

    /**
     * 全ての設定値のキャッシュが削除されます
     */
    public static void clearCacheAll() {
        VariablesService.clearCacheAll();
    }

    /**
     * 指定された設定値に関連するキャンペーン情報を元に効果測定用のイベント（message_open）を発火します。
     *
     * @param variables 設定値の配列
     */
    public static void trackOpen(@NotNull List<Variable> variables) {
        VariablesService.trackOpen(variables);
    }

    /**
     * 指定された設定値に関連するキャンペーン情報を元に効果測定用のイベント（message_open）を発火します。
     *
     * @param variables 設定値の配列
     * @param values    イベントに紐付けるカスタムオブジェクト
     */
    public static void trackOpen(@NotNull List<Variable> variables, @Nullable Map<String, ?> values) {
        VariablesService.trackOpen(variables, values);
    }

    /**
     * 指定された設定値に関連するキャンペーン情報を元に効果測定用のイベント（message_open）を発火します。
     *
     * @param variables  設定値の配列
     * @param jsonObject イベントに紐付けるカスタムオブジェクト
     */
    public static void trackOpen(@NotNull List<Variable> variables, @Nullable JSONObject jsonObject) {
        VariablesService.trackOpen(variables, jsonObject);
    }

    /**
     * 指定された設定値に関連するキャンペーン情報を元に効果測定用のイベント（message_click）を発火します。
     *
     * @param variables 設定値の配列
     */
    public static void trackClick(@NotNull List<Variable> variables) {
        VariablesService.trackClick(variables);
    }

    /**
     * 指定された設定値に関連するキャンペーン情報を元に効果測定用のイベント（message_click）を発火します。
     *
     * @param variables 設定値の配列
     * @param values    イベントに紐付けるカスタムオブジェクト
     */
    public static void trackClick(@NotNull List<Variable> variables, @Nullable Map<String, ?> values) {
        VariablesService.trackClick(variables, values);
    }

    /**
     * 指定された設定値に関連するキャンペーン情報を元に効果測定用のイベント（message_click）を発火します。
     *
     * @param variables  設定値の配列
     * @param jsonObject イベントに紐付けるカスタムオブジェクト
     */
    public static void trackClick(@NotNull List<Variable> variables, @Nullable JSONObject jsonObject) {
        VariablesService.trackClick(variables, jsonObject);
    }
}
