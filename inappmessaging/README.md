# KARTE アプリ内メッセージングモジュール

このドキュメントは、KARTE Android SDKのアプリ内メッセージングモジュールの詳細な説明を提供します。

## 目次

1. [概要](#概要)
2. [公開API](#公開api)
   - [InAppMessaging](#inappmessaging)
   - [InAppMessagingConfig](#inappmessagingconfig)
   - [InAppMessagingDelegate](#inappmessagingdelegate)
3. [内部実装](#内部実装)
   - [ExpiredMessageOpenEventRejectionFilterRule](#expiredmessageopeneventrejectionfilterrule)
   - [IAMProcessor](#iamprocessor)
   - [IAMWebView](#iamwebview)
   - [IAMWindow](#iamwindow)
   - [MessageModel](#messagemodel)
   - [PanelWindowManager](#panelwindowmanager)
   - [ResetPrevent](#resetprevent)
4. [JavaScript関連](#javascript関連)
   - [JsMessage](#jsmessage)
   - [State](#state)
5. [プレビュー関連](#プレビュー関連)
   - [PreviewParams](#previewparams)
6. [ビュー関連](#ビュー関連)
   - [AlertDialogFragment](#alertdialogfragment)
   - [BaseWebView](#basewebview)
   - [FileChooserFragment](#filechooserfragment)
   - [WindowView](#windowview)

## 概要

アプリ内メッセージングモジュールは、KARTEプラットフォームからのメッセージをアプリ内に表示するための機能を提供します。このモジュールを使用することで、ユーザーに対してカスタマイズされたメッセージやプロモーションを表示することができます。

## 公開API

### InAppMessaging

`InAppMessaging`クラスはアプリ内メッセージの管理を行う主要なクラスです。

#### クラスの目的と責任
- KARTEプラットフォームからのアプリ内メッセージを管理する
- メッセージの表示・非表示を制御する
- メッセージイベントを処理する

#### 継承関係とインターフェース
- `Library`インターフェースを実装：SDKのライブラリモジュールとして機能
- `ActionModule`インターフェースを実装：アクションの処理を担当
- `UserModule`インターフェースを実装：ユーザー関連の処理を担当
- `TrackModule`インターフェースを実装：トラッキング関連の処理を担当
- `ActivityLifecycleCallback`を継承：アクティビティのライフサイクルイベントを監視

#### メンバー変数
- `self`: 静的な自己参照
- `name`: ライブラリ名
- `app`: KARTEアプリケーションインスタンス
- `processor`: メッセージ処理を行う`IAMProcessor`インスタンス
- `config`: モジュールの設定を保持する`InAppMessagingConfig`インスタンス
- `uiThreadHandler`: UIスレッドでの処理を行うためのハンドラー
- `panelWindowManager`: パネルウィンドウを管理する`PanelWindowManager`インスタンス
- `isSuppressed`: メッセージ表示の抑制状態を示すフラグ
- `delegate`: イベント委譲先の`InAppMessagingDelegate`インスタンス

#### メソッド
- `configure(app: KarteApp)`: モジュールの初期化を行う
- `unconfigure(app: KarteApp)`: モジュールの終了処理を行う
- `receive(trackResponse: TrackResponse, trackRequest: TrackRequest)`: トラッキングレスポンスを受信し処理する
- `reset()`: 現在のページビューIDをリセットする
- `resetAll()`: すべての状態をリセットする
- `renewVisitorId(current: String, previous: String?)`: 訪問者IDが更新された際の処理を行う
- `prepare(event: Event)`: イベントの前処理を行う
- `intercept(request: TrackRequest)`: トラッキングリクエストの前処理を行う
- `onActivityStarted(activity: Activity)`: アクティビティ開始時の処理を行う
- `generateOverlayURL()`: オーバーレイURLを生成する
- `clearWebViewCookies()`: WebViewのクッキーをクリアする
- `trackMessageSuppressed(message: JSONObject, reason: String)`: メッセージ抑制イベントを記録する

#### 静的メソッド
- `isPresenting`: アプリ内メッセージが表示中かどうかを返す
- `delegate`: デリゲートの取得・設定を行う
- `dismiss()`: 表示中のすべてのアプリ内メッセージを非表示にする
- `suppress()`: アプリ内メッセージの表示を抑制する
- `unsuppress()`: アプリ内メッセージの表示抑制状態を解除する
- `registerPopupWindow(popupWindow: PopupWindow)`: PopupWindowを登録する
- `registerWindow(window: Window)`: Windowを登録する

### InAppMessagingConfig

`InAppMessagingConfig`クラスはInAppMessagingモジュールの設定を保持するクラスです。

#### クラスの目的と責任
- InAppMessagingモジュールの設定パラメータを管理する
- 設定値の取得・設定を提供する

#### 継承関係とインターフェース
- `LibraryConfig`インターフェースを実装：ライブラリ設定として機能

#### メンバー変数
- `_overlayBaseUrl`: オーバーレイベースURLを保持する内部変数

#### メソッド
- `overlayBaseUrl`: オーバーレイベースURLの取得・設定を行う

#### 内部クラス
- `Builder`: InAppMessagingConfigインスタンスを生成するためのビルダークラス
  - `overlayBaseUrl`: オーバーレイベースURLを設定する
  - `build()`: InAppMessagingConfigインスタンスを生成する

#### 静的メソッド
- `build(f: (Builder.() -> Unit)?)`: ビルダーパターンを使用してInAppMessagingConfigインスタンスを生成する

### InAppMessagingDelegate

`InAppMessagingDelegate`クラスはアプリ内メッセージで発生するイベントを委譲するための抽象クラスです。

#### クラスの目的と責任
- アプリ内メッセージのイベントをアプリケーションに通知する
- アプリケーションがイベントをカスタマイズするためのフックを提供する

#### メソッド
- `onWindowPresented()`: アプリ内メッセージ用のWindowが表示されたことを通知する
- `onWindowDismissed()`: アプリ内メッセージ用のWindowが非表示になったことを通知する
- `onPresented(campaignId: String, shortenId: String)`: 接客サービスアクションが表示されたことを通知する
- `onDismissed(campaignId: String, shortenId: String)`: 接客サービスアクションが非表示になったことを通知する
- `shouldOpenURL(url: Uri)`: 接客サービスアクション中のボタンがクリックされた際に、リンクをSDK側で自動的に処理するかどうか問い合わせる

## 内部実装

### ExpiredMessageOpenEventRejectionFilterRule

`ExpiredMessageOpenEventRejectionFilterRule`クラスは期限切れのメッセージオープンイベントを拒否するためのフィルタールールを提供します。

#### クラスの目的と責任
- 期限切れのメッセージオープンイベントをフィルタリングする

#### 継承関係とインターフェース
- `TrackEventRejectionFilterRule`インターフェースを実装：イベント拒否フィルタールールとして機能

#### メソッド
- `shouldReject(event: Event)`: イベントを拒否すべきかどうかを判断する

### IAMProcessor

`IAMProcessor`クラスはアプリ内メッセージの処理を行うクラスです。

#### クラスの目的と責任
- WebViewの管理
- メッセージの表示・非表示の制御
- アクティビティのライフサイクルイベントの処理

#### 継承関係とインターフェース
- `ActivityLifecycleCallback`を継承：アクティビティのライフサイクルイベントを監視
- `WebViewDelegate`インターフェースを実装：WebViewからのコールバックを処理

#### メンバー変数
- `container`: WebViewを管理するコンテナ
- `webView`: アプリ内メッセージを表示するためのWebView
- `window`: メッセージを表示するウィンドウ
- `currentActivity`: 現在のアクティビティへの弱参照
- `isWindowFocusByCross`: クロス表示キャンペーンによるフォーカス状態を示すフラグ
- `isWindowFocus`: ウィンドウのフォーカス状態を示すフラグ

#### メソッド
- `isPresenting`: メッセージが表示中かどうかを返す
- `teardown()`: リソースの解放を行う
- `handle(message: MessageModel)`: メッセージモデルを処理する
- `handleChangePv()`: ページビューの変更を処理する
- `handleView(values: JSONObject)`: ビューイベントを処理する
- `reset(isForce: Boolean)`: 状態をリセットする
- `reload(url: String?)`: WebViewをリロードする
- `setWindowFocus(message: MessageModel)`: ウィンドウのフォーカス状態を設定する
- `show(activity: Activity)`: メッセージを表示する
- `dismiss(withDelay: Boolean)`: メッセージを非表示にする
- WebViewDelegateインターフェースのメソッド実装
- ActivityLifecycleCallbackのメソッド実装

### IAMWebView

`IAMWebView`クラスはアプリ内メッセージを表示するためのWebViewを提供します。

#### クラスの目的と責任
- JavaScriptとのブリッジを提供する
- メッセージの表示を制御する
- WebViewの状態を管理する

#### 継承関係とインターフェース
- `BaseWebView`を継承：基本的なWebView機能を提供

#### メンバー変数
- `visible`: WebViewの可視状態を示すフラグ
- `uiThreadHandler`: UIスレッドでの処理を行うためのハンドラー
- `state`: WebViewの状態
- `queue`: 処理待ちのデータキュー
- `isReady`: WebViewが準備完了かどうかを示すフラグ
- `delegate`: WebViewDelegateインスタンス

#### メソッド
- `reload()`: WebViewをリロードする
- `reset(isForce: Boolean)`: 状態をリセットする
- `handleChangePv()`: ページビューの変更を処理する
- `handleView(values: JSONObject)`: ビューイベントを処理する
- `handleResponseData(data: String)`: レスポンスデータを処理する
- `changeState(newState: State)`: WebViewの状態を変更する
- `onReceivedMessage(name: String, data: String)`: JavaScriptからのメッセージを受信する
- `handleJsMessage(name: String, data: String)`: JavaScriptメッセージを処理する
- `notifyCampaignOpenOrClose(eventName: String, values: JSONObject)`: キャンペーンの開閉イベントを通知する
- `tryOpenUrl(uri: Uri, withReset: Boolean)`: URLを開く処理を試みる
- `setSafeAreaInset(top: Int)`: セーフエリアのインセットを設定する
- `errorOccurred()`: エラー発生時の処理を行う
- `openUrl(uri: Uri)`: URLを開く
- `showAlert(message: String)`: アラートを表示する
- `showFileChooser(filePathCallback: ValueCallback<Array<Uri>>)`: ファイル選択ダイアログを表示する

### IAMWindow

`IAMWindow`クラスはアプリ内メッセージを表示するためのウィンドウを提供します。

#### クラスの目的と責任
- メッセージを表示するためのウィンドウを管理する
- ウィンドウの表示・非表示を制御する

#### 継承関係とインターフェース
- `WindowView`を継承：基本的なウィンドウビュー機能を提供

#### メンバー変数
- `activity`: 関連するアクティビティ
- `isShowing`: ウィンドウが表示中かどうかを示すフラグ

#### メソッド
- `addView(child: View)`: ビューを追加する
- `show(focus: Boolean, view: View?)`: ウィンドウを表示する
- `dismiss(withDelay: Boolean)`: ウィンドウを非表示にする

### MessageModel

`MessageModel`クラスはアプリ内メッセージのデータモデルを提供します。

#### クラスの目的と責任
- メッセージデータの解析と管理
- メッセージの表示条件の判定

#### メンバー変数
- `data`: メッセージデータを保持するJSONObject
- `request`: 関連するトラッキングリクエスト
- `string`: Base64エンコードされたメッセージデータ文字列
- `messages`: メッセージのリスト

#### メソッド
- `filter(pvId: String, exclude: (JSONObject, String) -> Unit)`: メッセージをフィルタリングする
- `shouldLoad()`: メッセージをロードすべきかどうかを判断する
- `shouldFocus()`: ウィンドウにフォーカスを与えるべきかどうかを判断する
- `shouldFocusCrossDisplayCampaign()`: クロス表示キャンペーンにフォーカスを与えるべきかどうかを判断する
- `toString()`: メッセージの文字列表現を返す

### PanelWindowManager

`PanelWindowManager`クラスはパネルウィンドウを管理するクラスです。

#### クラスの目的と責任
- PopupWindowとWindowの登録と管理
- タッチイベントの分配

#### メンバー変数
- `lastActionDownWindow`: 最後にアクションダウンイベントを受け取ったウィンドウ
- `windows`: 登録されたウィンドウのリスト

#### メソッド
- `registerPopupWindow(popupWindow: PopupWindow)`: PopupWindowを登録する
- `registerWindow(window: Window)`: Windowを登録する
- `dispatchTouch(event: MotionEvent)`: タッチイベントを分配する

#### 内部クラス
- `BaseWindowWrapper`: ウィンドウラッパーの基本インターフェース
- `WindowWrapper`: Windowのラッパークラス
- `PopupWindowWrapper`: PopupWindowのラッパークラス

### ResetPrevent

`ResetPrevent`クラスはリセット防止機能を提供します。

#### クラスの目的と責任
- アクティビティの遷移時にリセットを防止する機能を提供する

#### メソッド
- リセット防止フラグの設定と取得を行うメソッド

## JavaScript関連

### JsMessage

`JsMessage`クラスはJavaScriptとのメッセージ交換を行うためのクラスです。

#### クラスの目的と責任
- JavaScriptからのメッセージを解析する
- メッセージタイプに応じた処理を行う

#### 継承関係
- 抽象クラス`JsMessage`を継承した複数のメッセージタイプクラスを提供

#### 内部クラス
- `Event`: イベントメッセージ
- `StateChanged`: 状態変更メッセージ
- `OpenUrl`: URL開封メッセージ
- `Visibility`: 可視性変更メッセージ
- `DocumentChanged`: ドキュメント変更メッセージ

#### メソッド
- `parse(name: String, data: String)`: メッセージを解析してJsMessageインスタンスを生成する

### State

`State`列挙型はWebViewの状態を表します。

#### 列挙値
- `LOADING`: ロード中
- `READY`: 準備完了
- `DESTROYED`: 破棄済み

#### メソッド
- `of(nameInCallback: String)`: コールバック名から状態を取得する

## プレビュー関連

### PreviewParams

`PreviewParams`クラスはプレビューパラメータを管理します。

#### クラスの目的と責任
- プレビューモードのパラメータを解析・管理する
- プレビューURLを生成する

#### メソッド
- `shouldShowPreview()`: プレビューを表示すべきかどうかを判断する
- `generateUrl(app: KarteApp)`: プレビューURLを生成する

## ビュー関連

### AlertDialogFragment

`AlertDialogFragment`クラスはアラートダイアログを表示するためのフラグメントです。

#### クラスの目的と責任
- アラートダイアログの表示を管理する

#### 継承関係
- `DialogFragment`を継承：ダイアログフラグメントとして機能

#### メソッド
- `show(activity: Activity, message: String)`: アラートダイアログを表示する

### BaseWebView

`BaseWebView`クラスは基本的なWebView機能を提供する抽象クラスです。

#### クラスの目的と責任
- WebViewの基本設定を行う
- WebViewClientとWebChromeClientの実装を提供する

#### 継承関係
- `WebView`を継承：Androidの標準WebView機能を拡張

#### メソッド
- `setSafeAreaInset(top: Int)`: セーフエリアのインセットを設定する
- `errorOccurred()`: エラー発生時の処理を行う
- `openUrl(uri: Uri)`: URLを開く
- `showAlert(message: String)`: アラートを表示する
- `showFileChooser(filePathCallback: ValueCallback<Array<Uri>>)`: ファイル選択ダイアログを表示する

### FileChooserFragment

`FileChooserFragment`クラスはファイル選択ダイアログを表示するためのフラグメントです。

#### クラスの目的と責任
- ファイル選択ダイアログの表示を管理する

#### 継承関係
- `Fragment`を継承：フラグメントとして機能

#### メソッド
- `showFileChooser(activity: Activity, filePathCallback: ValueCallback<Array<Uri>>)`: ファイル選択ダイアログを表示する

### WindowView

`WindowView`クラスはアプリ内メッセージを表示するためのビューを提供します。

#### クラスの目的と責任
- メッセージウィンドウの基本機能を提供する
- タッチイベントの処理を行う

#### 継承関係
- `FrameLayout`を継承：レイアウトコンテナとして機能

#### メソッド
- `show()`: ウィンドウを表示する
- `dismiss()`: ウィンドウを非表示にする
- `setFocus(focus: Boolean)`: フォーカス状態を設定する
- `updateTouchableRegions(touchableRegions: JSONArray)`: タッチ可能領域を更新する
