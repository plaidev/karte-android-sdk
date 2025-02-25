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

`InAppMessaging`クラスはKARTEプラットフォームからのアプリ内メッセージを管理する主要なクラスです。

#### クラスの目的と責任
- KARTEプラットフォームからのアプリ内メッセージを管理する
- メッセージの表示・非表示を制御する
- メッセージイベントを処理する
- アクティビティのライフサイクルに応じたメッセージ表示の制御
- アプリ内のポップアップウィンドウやダイアログとの連携

#### 技術的な詳細
- 複数のインターフェース（Library, ActionModule, UserModule, TrackModule）を実装し、SDKの様々な機能と連携
- シングルトンパターンを採用し、`self`静的変数を通じてグローバルにアクセス可能
- ActivityLifecycleCallbackを継承してアプリのライフサイクルイベントを監視し、適切なタイミングでメッセージを表示
- UIスレッドハンドラーを使用して非同期処理を実現し、UIスレッド上でメッセージ表示処理を行う
- WebViewを使用してHTMLベースのメッセージを表示（IAMProcessorを通じて）
- PanelWindowManagerを使用してタッチイベントを適切に処理し、アプリのUIとメッセージの両方を操作可能に
- 訪問者ID変更時にWebViewのクッキーをクリアして状態をリセット
- プレビューモードをサポートし、開発者がメッセージをテストできる機能を提供
- メッセージ表示の抑制機能により、アプリの重要な操作中にメッセージが表示されないよう制御可能

#### 継承関係とインターフェース
- `Library`インターフェースを実装：SDKのライブラリモジュールとして機能し、初期化と終了処理を担当
- `ActionModule`インターフェースを実装：アクションの処理を担当し、サーバーからのレスポンスを処理
- `UserModule`インターフェースを実装：ユーザー関連の処理を担当し、訪問者IDの変更に対応
- `TrackModule`インターフェースを実装：トラッキング関連の処理を担当し、イベントの前処理を行う
- `ActivityLifecycleCallback`を継承：アクティビティのライフサイクルイベントを監視し、プレビューモードの検出などを行う

#### メンバー変数
- `self`: 静的な自己参照（シングルトンパターンの実装）
- `name`: ライブラリ名（識別子として使用）
- `app`: KARTEアプリケーションインスタンス（設定やアプリ情報へのアクセスに使用）
- `processor`: メッセージ処理を行う`IAMProcessor`インスタンス（実際のメッセージ表示処理を担当）
- `config`: モジュールの設定を保持する`InAppMessagingConfig`インスタンス（設定値へのアクセスに使用）
- `uiThreadHandler`: UIスレッドでの処理を行うためのハンドラー（非同期処理の実現）
- `panelWindowManager`: パネルウィンドウを管理する`PanelWindowManager`インスタンス（タッチイベント処理）
- `isSuppressed`: メッセージ表示の抑制状態を示すフラグ（表示制御に使用）
- `delegate`: イベント委譲先の`InAppMessagingDelegate`インスタンス（イベント通知）

#### メソッド
- `configure(app: KarteApp)`: モジュールの初期化を行う（アプリケーションへの登録、プロセッサーの初期化）
- `unconfigure(app: KarteApp)`: モジュールの終了処理を行う（リソースの解放、登録解除）
- `receive(trackResponse: TrackResponse, trackRequest: TrackRequest)`: トラッキングレスポンスを受信し処理する（メッセージの表示判定）
- `reset()`: 現在のページビューIDをリセットする（画面遷移時の処理）
- `resetAll()`: すべての状態をリセットする（完全なリセット）
- `renewVisitorId(current: String, previous: String?)`: 訪問者IDが更新された際の処理を行う（状態のリセットとリロード）
- `prepare(event: Event)`: イベントの前処理を行う（viewイベントの処理）
- `intercept(request: TrackRequest)`: トラッキングリクエストの前処理を行う
- `onActivityStarted(activity: Activity)`: アクティビティ開始時の処理を行う（プレビューモードの判定）
- `generateOverlayURL()`: オーバーレイURLを生成する（WebView表示用URL生成）
- `clearWebViewCookies()`: WebViewのクッキーをクリアする（訪問者ID変更時の処理）
- `trackMessageSuppressed(message: JSONObject, reason: String)`: メッセージ抑制イベントを記録する（抑制理由の追跡）

#### 静的メソッド
- `isPresenting`: アプリ内メッセージが表示中かどうかを返す（表示状態の確認）
- `delegate`: デリゲートの取得・設定を行う（イベント委譲先の管理）
- `dismiss()`: 表示中のすべてのアプリ内メッセージを非表示にする（強制非表示）
- `suppress()`: アプリ内メッセージの表示を抑制する（表示抑制モードの有効化）
- `unsuppress()`: アプリ内メッセージの表示抑制状態を解除する（表示抑制モードの無効化）
- `registerPopupWindow(popupWindow: PopupWindow)`: PopupWindowを登録する（タッチイベント処理用）
- `registerWindow(window: Window)`: Windowを登録する（タッチイベント処理用）

#### 使用例

##### メッセージ表示の制御
アプリ開発者がユーザーの操作を妨げないようにアプリ内メッセージの表示を制御したい場合、特定の画面や状況でメッセージ表示を一時的に抑制することができます。例えば、ユーザーが決済処理を行っている間はメッセージを表示しないようにするには：

```kotlin
// 決済画面の開始時
override fun onResume() {
    super.onResume()
    InAppMessaging.suppress() // メッセージ表示を抑制
}

// 決済完了後
private fun onPaymentCompleted() {
    InAppMessaging.unsuppress() // メッセージ表示の抑制を解除
}
```

##### ポップアップウィンドウとの連携
アプリ内で表示しているダイアログやポップアップウィンドウとアプリ内メッセージが適切に連携するように登録することもできます：

```kotlin
// カスタムダイアログの表示
private fun showCustomDialog() {
    val dialog = Dialog(this)
    dialog.setContentView(R.layout.custom_dialog)
    dialog.show()
    
    // ダイアログのウィンドウをKARTEに登録して適切なタッチイベント処理を実現
    InAppMessaging.registerWindow(dialog.window!!)
}
```

##### メッセージイベントの処理
アプリ内メッセージの表示・非表示イベントを処理するには、デリゲートを設定します：

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // KARTEの初期化
        val config = KarteConfig.Builder()
            .applicationKey("YOUR_APP_KEY")
            .build()
        Karte.setup(this, config)
        
        // InAppMessagingデリゲートの設定
        InAppMessaging.delegate = object : InAppMessagingDelegate() {
            override fun onWindowPresented() {
                // メッセージウィンドウが表示された時の処理
                Log.d("KARTE", "メッセージウィンドウが表示されました")
            }
            
            override fun onWindowDismissed() {
                // メッセージウィンドウが非表示になった時の処理
                Log.d("KARTE", "メッセージウィンドウが非表示になりました")
            }
            
            override fun shouldOpenURL(url: Uri): Boolean {
                // URLを開く前の処理（trueを返すとSDKが自動的に処理、falseを返すとアプリ側で処理）
                if (url.host == "myapp.example.com") {
                    // アプリ内の特定のURLはアプリ側で処理
                    handleDeepLink(url)
                    return false
                }
                return true // その他のURLはSDKに処理を委譲
            }
        }
    }
}
```

### InAppMessagingConfig

`InAppMessagingConfig`クラスはInAppMessagingモジュールの設定を保持するクラスです。

#### クラスの目的と責任
- InAppMessagingモジュールの設定パラメータを管理する
- 設定値の取得・設定を提供する
- アプリ内メッセージのオーバーレイベースURLを制御する
- デフォルト値の管理と設定値の検証を行う

#### 技術的な詳細
- `LibraryConfig`インターフェースを実装し、KARTEのライブラリ設定システムと統合
- ビルダーパターンを採用し、設定の柔軟な構築を可能に
- プライベートコンストラクタを使用して直接インスタンス化を防止し、ビルダー経由での生成を強制
- Kotlinのプロパティ機能を活用したゲッターとセッターによる値の検証と変換
- デフォルト値（`OVERLAY_DEFAULT_URL = "https://cf-native.karte.io"`）を提供し、未設定時の動作を保証
- 空文字チェックによる不正な設定値の防止
- Javaとの互換性を考慮した設計（`@JvmSynthetic`アノテーションの使用）

#### 継承関係とインターフェース
- `LibraryConfig`インターフェースを実装：KARTEのライブラリ設定システムの一部として機能

#### メンバー変数
- `_overlayBaseUrl`: オーバーレイベースURLを保持する内部変数（直接アクセスされない）

#### メソッド
- `overlayBaseUrl`: オーバーレイベースURLの取得・設定を行うプロパティ
  - ゲッター: 値が空の場合はデフォルト値を返す
  - セッター: 空文字チェックを行い、有効な値のみを設定

#### 内部クラス
- `Builder`: InAppMessagingConfigインスタンスを生成するためのビルダークラス
  - `overlayBaseUrl`: オーバーレイベースURLを設定するプロパティとメソッド
  - `build()`: 設定された値を使用してInAppMessagingConfigインスタンスを生成

#### 静的メソッド
- `build(f: (Builder.() -> Unit)?)`: ビルダーパターンを使用してInAppMessagingConfigインスタンスを生成する
  - Kotlinの関数型引数とスコープ関数を活用した簡潔な設定構文を提供

#### 使用例

##### 基本的な設定
通常のアプリ開発では、デフォルト設定のままInAppMessagingを使用するため、特別な設定は不要です：

```kotlin
// KARTEの初期化時にデフォルト設定を使用
val config = KarteConfig.Builder()
    .applicationKey("YOUR_APP_KEY")
    .build()
Karte.setup(this, config)
```

##### カスタム設定（開発・テスト環境向け）
開発環境やテスト環境で異なるオーバーレイサーバーを使用する場合：

```kotlin
// カスタムオーバーレイURLを設定
val inAppMessagingConfig = InAppMessagingConfig.build {
    overlayBaseUrl = "https://dev-cf-native.karte.io" // 開発環境用URL
}

// KARTEの初期化時に設定を適用
val config = KarteConfig.Builder()
    .applicationKey("YOUR_APP_KEY")
    .libraryConfig(inAppMessagingConfig) // カスタム設定を適用
    .build()
Karte.setup(this, config)
```

##### Javaでの使用例
Javaからも同様に設定可能です：

```java
// Javaでのカスタム設定
InAppMessagingConfig inAppMessagingConfig = InAppMessagingConfig.build(builder -> {
    builder.overlayBaseUrl("https://staging-cf-native.karte.io"); // ステージング環境用URL
    return null;
});

// KARTEの初期化時に設定を適用
KarteConfig config = new KarteConfig.Builder()
    .applicationKey("YOUR_APP_KEY")
    .libraryConfig(inAppMessagingConfig) // カスタム設定を適用
    .build();
Karte.setup(this, config);
```

### InAppMessagingDelegate

`InAppMessagingDelegate`クラスはアプリ内メッセージで発生するイベントを委譲するための抽象クラスです。

#### クラスの目的と責任
- アプリ内メッセージのイベントをアプリケーションに通知する
- アプリケーションがイベントをカスタマイズするためのフックを提供する
- メッセージの表示・非表示のタイミングをアプリに伝える
- URLの処理方法をアプリ側で制御できるようにする
- アプリとKARTEのメッセージングシステム間の橋渡しをする

#### 技術的な詳細
- 抽象クラスとして設計され、必要なメソッドのみをオーバーライドして使用可能
- すべてのメソッドにデフォルト実装が提供されており、必要なコールバックのみを実装可能
- `open`修飾子を使用し、サブクラスでのオーバーライドを許可
- イベント通知メソッドは戻り値を持たず、単純な通知として機能
- `shouldOpenURL`メソッドのみboolean値を返し、アプリ側での処理か、SDK側での処理かを制御
- `InAppMessaging.delegate`プロパティを通じて設定され、シングルトンパターンと組み合わせて使用
- キャンペーンIDと短縮IDを提供し、アプリ側でのイベント追跡や分析を可能に

#### メソッド
- `onWindowPresented()`: アプリ内メッセージ用のWindowが表示されたことを通知する
  - メッセージウィンドウが画面に表示された直後に呼び出される
  - アプリのUI調整やログ記録などに使用可能
- `onWindowDismissed()`: アプリ内メッセージ用のWindowが非表示になったことを通知する
  - メッセージウィンドウが画面から消えた直後に呼び出される
  - アプリのUI状態の復元などに使用可能
- `onPresented(campaignId: String, shortenId: String)`: 接客サービスアクションが表示されたことを通知する
  - 特定のキャンペーンのメッセージが表示された時に呼び出される
  - キャンペーンIDと短縮IDを提供し、アプリ側での識別を可能に
- `onDismissed(campaignId: String, shortenId: String)`: 接客サービスアクションが非表示になったことを通知する
  - 特定のキャンペーンのメッセージが非表示になった時に呼び出される
  - キャンペーンIDと短縮IDを提供し、アプリ側での識別を可能に
- `shouldOpenURL(url: Uri)`: 接客サービスアクション中のボタンがクリックされた際に、リンクをSDK側で自動的に処理するかどうか問い合わせる
  - メッセージ内のリンクがクリックされた時に呼び出される
  - `true`を返すとSDKがリンクを自動的に開き、`false`を返すとアプリ側で処理する必要がある

#### 使用例

##### 基本的な実装
アプリ内メッセージのイベントをログに記録する基本的な実装：

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // KARTEの初期化
        val config = KarteConfig.Builder()
            .applicationKey("YOUR_APP_KEY")
            .build()
        Karte.setup(this, config)
        
        // InAppMessagingデリゲートの設定
        InAppMessaging.delegate = object : InAppMessagingDelegate() {
            override fun onWindowPresented() {
                Log.d("KARTE", "メッセージウィンドウが表示されました")
            }
            
            override fun onWindowDismissed() {
                Log.d("KARTE", "メッセージウィンドウが非表示になりました")
            }
            
            override fun onPresented(campaignId: String, shortenId: String) {
                Log.d("KARTE", "キャンペーン表示: ID=$campaignId, 短縮ID=$shortenId")
            }
            
            override fun onDismissed(campaignId: String, shortenId: String) {
                Log.d("KARTE", "キャンペーン非表示: ID=$campaignId, 短縮ID=$shortenId")
            }
        }
    }
}
```

##### URLハンドリングのカスタマイズ
アプリ内メッセージからのリンクを独自に処理する実装：

```kotlin
InAppMessaging.delegate = object : InAppMessagingDelegate() {
    override fun shouldOpenURL(url: Uri): Boolean {
        // アプリ内のディープリンクを処理
        when {
            url.host == "myapp.example.com" -> {
                // アプリ内の特定の画面に遷移
                val intent = Intent(applicationContext, DetailActivity::class.java).apply {
                    data = url
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                return false // SDKによる処理を行わない
            }
            url.toString().startsWith("https://docs.example.com") -> {
                // アプリ内のWebViewで表示
                val intent = Intent(applicationContext, WebViewActivity::class.java).apply {
                    putExtra("url", url.toString())
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                return false // SDKによる処理を行わない
            }
            else -> {
                // その他のURLはSDKに処理を委譲
                return true
            }
        }
    }
}
```

##### メッセージ表示に応じたアプリの動作調整
メッセージの表示・非表示に応じてアプリの動作を調整する実装：

```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var backgroundMusicPlayer: MediaPlayer
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // BGM初期化
        backgroundMusicPlayer = MediaPlayer.create(this, R.raw.background_music)
        backgroundMusicPlayer.isLooping = true
        
        // InAppMessagingデリゲートの設定
        InAppMessaging.delegate = object : InAppMessagingDelegate() {
            override fun onWindowPresented() {
                // メッセージ表示時にBGMの音量を下げる
                backgroundMusicPlayer.setVolume(0.3f, 0.3f)
            }
            
            override fun onWindowDismissed() {
                // メッセージ非表示時にBGMの音量を元に戻す
                backgroundMusicPlayer.setVolume(1.0f, 1.0f)
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        backgroundMusicPlayer.start()
    }
    
    override fun onPause() {
        super.onPause()
        backgroundMusicPlayer.pause()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        backgroundMusicPlayer.release()
    }
}
```

## 内部実装

### ExpiredMessageOpenEventRejectionFilterRule

`ExpiredMessageOpenEventRejectionFilterRule`クラスは、期限切れのメッセージオープンイベントをトラッキングシステムから除外するためのフィルタールールです。アプリ内メッセージが表示されてから一定時間（デフォルトでは3分）が経過した後のオープンイベントを無視することで、データの正確性を確保します。

#### クラスの目的と責任
- 古いメッセージオープンイベントをトラッキングシステムから除外する
- メッセージのレスポンスタイムスタンプと現在時刻を比較して有効期限を判定する
- 無効なタイムスタンプ形式や欠落したタイムスタンプを適切に処理する
- トラッキングデータの品質と正確性を確保する
- サーバーへの不要なリクエストを減らしネットワーク負荷を軽減する

#### 技術的な詳細
- `TrackEventRejectionFilterRule`インターフェースを実装し、KARTEのトラッキングフィルタリングシステムと統合
- `SimpleDateFormat`を使用したISO 8601形式のタイムスタンプ解析
- `runCatching`と`fold`を使用した例外安全な日付処理
- デフォルトで180秒（3分）の有効期限を設定
- 依存性注入パターンを使用した現在時刻の取得（テスト容易性向上）
- GMTタイムゾーンを使用した日付処理による国際的な一貫性の確保
- メッセージイベントの`response_timestamp`フィールドを使用した経過時間の計算
- 秒単位の時間比較による高精度な有効期限チェック
- JSONオブジェクトの安全な操作（`optJSONObject`、`optString`）によるNullPointer例外の回避

#### 継承関係とインターフェース
- `TrackEventRejectionFilterRule`インターフェースを実装：KARTEのトラッキングシステムのフィルタールールとして機能

#### メンバー変数
- `interval`: メッセージの有効期限（秒単位）、デフォルトは180秒（3分）
- `dateResolver`: 現在時刻を取得するための関数型プロパティ、デフォルトは`{ Date() }`
- `libraryName`: フィルタールールが属するライブラリ名（InAppMessaging.name）
- `eventName`: フィルタリング対象のイベント名（MessageEventName.MessageOpen）

#### 静的メンバー
- `dateFormatter`: ISO 8601形式のタイムスタンプを解析するための`SimpleDateFormat`インスタンス
  - フォーマット: `yyyy-MM-dd'T'HH:mm:ss.SSSZ`
  - タイムゾーン: GMT

#### メソッド
- `reject(event: Event)`: 指定されたイベントを拒否するかどうかを判断する
  - イベントから`message.response_timestamp`を抽出
  - タイムスタンプが空の場合は拒否しない（false）
  - タイムスタンプを日付に変換
  - 現在時刻とレスポンスタイムスタンプの差が`interval`秒を超える場合は拒否（true）
  - 日付変換に失敗した場合は拒否しない（false）

#### 使用例

このクラスは内部クラスであり、直接アプリ開発者が使用することはありませんが、以下のようにKARTEのトラッキングシステム内部で使用されます：

```kotlin
// InAppMessagingクラス内での使用例（実際の実装の簡略版）
internal class InAppMessaging : Library, ActionModule, UserModule, TrackModule {
    // 初期化時にフィルタールールを登録
    override fun configure(app: KarteApp) {
        // 他の初期化処理...
        
        // 期限切れメッセージオープンイベントのフィルタールールを登録
        val filterRule = ExpiredMessageOpenEventRejectionFilterRule()
        app.tracker.registerTrackEventRejectionFilterRule(filterRule)
    }
    
    // その他のメソッド...
}
```

実際の動作フローは以下のようになります：

1. アプリ内メッセージが表示される
2. メッセージには`response_timestamp`（サーバーからのレスポンス時刻）が含まれている
3. ユーザーがメッセージを開く（クリックする）と`message_open`イベントが生成される
4. イベントがトラッキングシステムに送信される前に`ExpiredMessageOpenEventRejectionFilterRule`によってフィルタリングされる
5. 現在時刻とメッセージの`response_timestamp`の差が3分（180秒）を超える場合、イベントは拒否される
6. 拒否されたイベントはサーバーに送信されず、ローカルで破棄される

このフィルタリングメカニズムにより、以下のような利点があります：

1. **データの正確性向上**: 古いメッセージに対するアクションが誤ってカウントされるのを防ぎます
2. **サーバー負荷の軽減**: 無意味なイベントデータがサーバーに送信されるのを防ぎます
3. **分析精度の向上**: マーケティング分析において、実際のユーザー行動をより正確に反映したデータを提供します

例えば、ユーザーがアプリを長時間バックグラウンドに置いた後に再開し、古いメッセージをクリックした場合、そのクリックイベントは既に期限切れとみなされ、トラッキングシステムから除外されます。これにより、メッセージの効果測定がより正確になります。

### IAMProcessor

`IAMProcessor`クラスはアプリ内メッセージの処理を行う内部クラスです。InAppMessagingクラスとWebView間の橋渡し役として機能します。

#### クラスの目的と責任
- WebViewの管理とライフサイクル制御
- メッセージの表示・非表示の制御
- アクティビティのライフサイクルイベントの処理
- WebViewからのコールバックの処理
- メッセージウィンドウの管理
- フォーカス状態の管理

#### 技術的な詳細
- `ActivityLifecycleCallback`を継承し、アプリのライフサイクルに合わせてメッセージの表示・非表示を制御
- `WebViewDelegate`インターフェースを実装し、WebViewからのイベントを処理
- `WeakReference`を使用して`Activity`への参照を保持し、メモリリークを防止
- `WebViewContainer`内部クラスを使用してWebViewのインスタンス管理を行い、必要に応じて再生成
- アプリがバックグラウンドに移行する際にメッセージをリセットし、フォアグラウンドに戻った際に再表示
- メッセージの表示状態に応じてウィンドウのフォーカス状態を制御
- クロス表示キャンペーン（複数画面にまたがるメッセージ）の特別な処理をサポート
- ファイル選択やアラート表示などのUIイベントを現在のアクティビティに委譲
- URLの処理をInAppMessagingDelegateに委譲し、アプリ側での制御を可能に
- エラー発生時の適切な処理とリソース解放

#### 継承関係とインターフェース
- `ActivityLifecycleCallback`を継承：アクティビティのライフサイクルイベントを監視し、適切なタイミングでメッセージを表示・非表示
- `WebViewDelegate`インターフェースを実装：WebViewからのコールバックを処理し、必要なアクションを実行

#### メンバー変数
- `container`: WebViewを管理するコンテナ（WebViewの生成・破棄・再利用を担当）
- `webView`: アプリ内メッセージを表示するためのWebView（getterを通じてcontainerから取得）
- `window`: メッセージを表示するウィンドウ（IAMWindowインスタンス）
- `currentActivity`: 現在のアクティビティへの弱参照（メモリリーク防止）
- `isWindowFocusByCross`: クロス表示キャンペーンによるフォーカス状態を示すフラグ
- `isWindowFocus`: ウィンドウのフォーカス状態を示すフラグ
- `panelWindowManager`: パネルウィンドウマネージャー（タッチイベント処理用）

#### メソッド
- `isPresenting`: メッセージが表示中かどうかを返す（現在のウィンドウ表示状態を確認）
- `teardown()`: リソースの解放を行う（WebViewの破棄、ウィンドウの非表示、参照のクリア）
- `handle(message: MessageModel)`: メッセージモデルを処理する（フォーカス設定とWebViewへのデータ渡し）
- `handleChangePv()`: ページビューの変更を処理する（WebViewに通知）
- `handleView(values: JSONObject)`: ビューイベントを処理する（WebViewに通知）
- `reset(isForce: Boolean)`: 状態をリセットする（WebViewのリセット）
- `reload(url: String?)`: WebViewをリロードする（URLが変更された場合は再生成）
- `setWindowFocus(message: MessageModel)`: ウィンドウのフォーカス状態を設定する（メッセージの種類に応じて判定）
- `show(activity: Activity)`: メッセージを表示する（ウィンドウの生成と表示）
- `dismiss(withDelay: Boolean)`: メッセージを非表示にする（ウィンドウの非表示と参照のクリア）

#### WebViewDelegateインターフェースの実装
- `onWebViewVisible()`: WebViewが可視状態になった時の処理（ウィンドウの表示）
- `onWebViewInvisible()`: WebViewが不可視状態になった時の処理（ウィンドウの非表示）
- `onUpdateTouchableRegions(touchableRegions: JSONArray)`: タッチ可能領域の更新（ウィンドウに通知）
- `shouldOpenUrl(uri: Uri)`: URLを開くべきかの判定（デリゲートに委譲）
- `onOpenUrl(uri: Uri, withReset: Boolean)`: URLを開く処理（リセット防止フラグの設定とURL開封）
- `onErrorOccurred()`: エラー発生時の処理（リセットと非表示）
- `onShowAlert(message: String)`: アラート表示（AlertDialogFragmentを使用）
- `onShowFileChooser(filePathCallback: ValueCallback<Array<Uri>>)`: ファイル選択ダイアログの表示

#### ActivityLifecycleCallbackの実装
- `onActivityResumed(activity: Activity)`: アクティビティ再開時の処理（現在のアクティビティの更新とメッセージ表示）
- `onActivityPaused(activity: Activity)`: アクティビティ一時停止時の処理（リセット防止フラグに基づく処理）

#### 内部クラス: WebViewContainer
- WebViewのインスタンス管理を担当
- 必要に応じてWebViewを生成・破棄・再生成
- WebViewの初期化とURL読み込みを処理
- WebView生成時の例外処理（WebViewの更新中やその他の問題に対応）

#### 使用例

IAMProcessorは内部クラスであり、直接アプリ開発者が使用することはありませんが、以下のようにInAppMessagingクラスを通じて間接的に利用されます：

```kotlin
// アプリ内メッセージの表示制御
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // ユーザーアクションのトラッキング（これによりIAMProcessorが内部的に起動し、メッセージを表示）
        findViewById<Button>(R.id.action_button).setOnClickListener {
            Tracker.track("button_clicked")
        }
        
        // 特定の画面でメッセージを表示しない場合
        findViewById<Button>(R.id.checkout_button).setOnClickListener {
            // チェックアウト画面に遷移する前にメッセージを抑制
            InAppMessaging.suppress()
            startActivity(Intent(this, CheckoutActivity::class.java))
        }
    }
}

// チェックアウト完了後にメッセージ表示を再開
class CheckoutActivity : AppCompatActivity() {
    override fun onBackPressed() {
        super.onBackPressed()
        // チェックアウト画面から戻る際にメッセージ表示を再開
        InAppMessaging.unsuppress()
    }
}
```

内部的には、IAMProcessorはアクティビティのライフサイクルに応じてメッセージの表示状態を管理します。例えば、アプリがバックグラウンドに移行する際にはメッセージを非表示にし、フォアグラウンドに戻った際には再表示します。また、WebViewからのイベント（リンククリックなど）を適切に処理し、必要に応じてアプリ側のデリゲートに通知します。

### IAMWebView

`IAMWebView`クラスはアプリ内メッセージを表示するためのWebViewクラスです。JavaScriptとネイティブコード間の橋渡しを担当し、メッセージの表示制御やイベント処理を行います。

#### クラスの目的と責任
- アプリ内メッセージのHTML/CSSコンテンツを表示する
- JavaScriptとネイティブコード間の通信を処理する
- WebViewの状態管理を行う
- メッセージの表示・非表示を制御する
- ユーザーインタラクションを処理する
- イベントをトラッキングシステムに送信する
- デリゲートを通じてアプリに通知する

#### 技術的な詳細
- `BaseWebView`を継承し、WebViewの基本機能を拡張
- `@SuppressLint("ViewConstructor", "AddJavascriptInterface")`アノテーションを使用して、Androidのlintチェックを抑制
- JavaScriptインターフェース（`NativeBridge`）を使用して、WebViewとネイティブコード間の双方向通信を実現
- `Handler`を使用してUIスレッドでの処理を保証
- 状態管理システム（`State`列挙型）を使用して、WebViewの準備状態を追跡
- キューイングメカニズムを実装し、WebViewが準備完了前に受信したデータを保存して後で処理
- JavaScriptメッセージのパース処理を実装し、様々なタイプのメッセージ（イベント、状態変更、URL開封など）を処理
- タッチ可能領域の更新機能を提供し、複雑なインタラクティブUIをサポート
- セーフエリアのインセット設定をサポートし、異なるデバイス画面に対応
- エラー処理とリカバリーメカニズムを実装
- ファイル選択やアラート表示などのUIインタラクションをデリゲートに委譲

#### 継承関係とインターフェース
- `BaseWebView`を継承：WebViewの基本機能と設定を提供
- `WebViewDelegate`インターフェースを使用：WebViewからのコールバックをIAMProcessorに委譲

#### メンバー変数
- `visible`: WebViewの可視状態を示すフラグ（メッセージが表示されているかどうか）
- `uiThreadHandler`: UIスレッドでの処理を行うためのハンドラ（スレッドセーフな操作を保証）
- `state`: WebViewの現在の状態（LOADING, READY, ERROR, DESTROYED）
- `queue`: 処理待ちのデータキュー（WebViewが準備完了前に受信したデータを保存）
- `isReady`: WebViewが準備完了状態かどうかを示すフラグ（state == State.READYの簡易アクセサ）
- `delegate`: WebViewDelegateインターフェースの実装（IAMProcessor）への参照

#### メソッド
- `reload()`: WebViewをリロードし、状態をLOADINGにリセット
- `reset(isForce: Boolean)`: JavaScriptの状態をリセット（強制リセットフラグ付き）
- `handleChangePv()`: ページビューの変更をJavaScriptに通知
- `handleView(values: JSONObject)`: ビューイベントをJavaScriptに通知（準備完了前はキューに保存）
- `handleResponseData(data: String)`: サーバーからのレスポンスデータをJavaScriptに渡す（準備完了前はキューに保存）
- `changeState(newState: State)`: WebViewの状態を変更し、状態に応じた処理を実行（キューの処理など）
- `onReceivedMessage(name: String, data: String)`: JavaScriptからのメッセージを受信するJavaScriptインターフェースメソッド
- `handleJsMessage(name: String, data: String)`: JavaScriptからのメッセージを種類に応じて処理
  - イベントメッセージ：トラッキングシステムにイベントを送信
  - 状態変更メッセージ：WebViewの状態を更新
  - URL開封メッセージ：URLを開く処理を実行
  - ドキュメント変更メッセージ：タッチ可能領域を更新
  - 可視性メッセージ：WebViewの表示・非表示を制御
- `notifyCampaignOpenOrClose(eventName: String, values: JSONObject)`: キャンペーンの開閉イベントをデリゲートに通知
- `tryOpenUrl(uri: Uri, withReset: Boolean)`: URLを開く処理を試み、デリゲートの判断に基づいて実行
- `setSafeAreaInset(top: Int)`: セーフエリアのインセットをJavaScriptに通知
- `errorOccurred()`: エラー発生時にデリゲートに通知
- `openUrl(uri: Uri)`: URLを開く処理をtryOpenUrlに委譲
- `showAlert(message: String)`: アラート表示をデリゲートに委譲
- `showFileChooser(filePathCallback: ValueCallback<Array<Uri>>)`: ファイル選択ダイアログの表示をデリゲートに委譲

#### 内部クラス: Data
- `ResponseData`: サーバーからのレスポンスデータを保持するデータクラス
- `ViewData`: ビューイベントのデータを保持するデータクラス

#### WebViewDelegateインターフェース
- WebViewからのコールバックをIAMProcessorに委譲するためのインターフェース
- WebViewの可視性変更、タッチ可能領域の更新、URL処理、エラー処理などのメソッドを定義

#### 使用例

IAMWebViewは内部クラスであり、直接アプリ開発者が使用することはありませんが、以下のようにInAppMessagingシステム内部で使用されます：

```kotlin
// IAMProcessorクラス内での使用例（実際の実装の簡略版）
internal class IAMProcessor(application: Application, private val panelWindowManager: PanelWindowManager) : ActivityLifecycleCallback(), WebViewDelegate {
    private val container = WebViewContainer(application, this)
    private val webView: IAMWebView?
        get() = container.get()
    
    // メッセージモデルの処理
    fun handle(message: MessageModel) {
        setWindowFocus(message)
        webView?.handleResponseData(message.string) // IAMWebViewにデータを渡す
    }
    
    // WebViewDelegateの実装
    override fun onWebViewVisible() {
        currentActivity?.get()?.let {
            show(it) // WebViewが可視になったらウィンドウを表示
        }
    }
    
    override fun onWebViewInvisible() {
        dismiss() // WebViewが不可視になったらウィンドウを非表示
    }
    
    // その他のWebViewDelegateメソッドの実装...
}
```

内部的には、IAMWebViewはJavaScriptとネイティブコード間の通信を管理し、メッセージの表示状態やユーザーインタラクションを処理します。例えば、メッセージ内のボタンがクリックされると、JavaScriptからのメッセージを受信し、適切なアクションを実行します。また、アプリの状態変化（ページビューの変更など）をJavaScriptに通知し、メッセージの表示制御を行います。

WebViewの状態管理も重要な役割で、準備完了前に受信したデータをキューに保存し、準備完了後に処理することで、タイミングの問題を解決しています。また、エラー処理やリカバリーメカニズムも実装されており、WebViewの問題が発生した場合でも適切に対応します。

### IAMWindow

`IAMWindow`クラスはアプリ内メッセージを表示するためのウィンドウを提供します。WebViewコンテンツを表示するためのコンテナとして機能し、ウィンドウの表示・非表示の制御やビューの管理を担当します。

#### クラスの目的と責任
- メッセージを表示するためのウィンドウを管理する
- ウィンドウの表示・非表示を制御する
- WebViewをウィンドウに追加・削除する
- フォーカス状態を管理する
- デリゲートに対してウィンドウの表示・非表示イベントを通知する
- タッチ可能領域の更新を処理する

#### 技術的な詳細
- `WindowView`を継承し、アプリ内メッセージ表示に特化した機能を追加
- `@SuppressLint("ViewConstructor")`アノテーションを使用して、Androidのlintチェックを抑制
- `@TargetApi(Build.VERSION_CODES.KITKAT)`アノテーションを使用して、API 19以上での動作を保証
- 親ビューからの子ビューの自動削除処理を実装し、ビューの再利用時の問題を防止
- 遅延ディスミス機能を提供し、アクティビティのライフサイクルイベント中の安全な非表示処理を実現
- `InAppMessagingDelegate`を通じてウィンドウの表示・非表示イベントをアプリに通知
- `PanelWindowManager`と連携してタッチイベントの適切な処理を実現
- ウィンドウの表示状態を`visibility`と`isAttachedToWindow`の両方で判定し、正確な状態管理を実現

#### 継承関係とインターフェース
- `WindowView`を継承：基本的なウィンドウビュー機能と、タッチイベント処理、タッチ可能領域の管理などを提供

#### メンバー変数
- `activity`: 関連するアクティビティへの参照（ウィンドウの表示コンテキストとして使用）
- `isShowing`: ウィンドウが表示中かどうかを示すプロパティ（`visibility`と`isAttachedToWindow`の状態に基づいて判定）

#### メソッド
- `addView(child: View)`: ビューをウィンドウに追加する
  - 既に親ビューが存在する場合は、自動的に親ビューから削除してから追加
  - レイアウトパラメータを適切に設定して全画面表示を実現
- `show(focus: Boolean, view: View?)`: ウィンドウを表示する
  - フォーカスフラグに基づいてウィンドウのフォーカス状態を設定
  - 指定されたビュー（WebView）をウィンドウに追加
  - 親クラスの`show()`メソッドを呼び出してウィンドウを表示
  - デリゲートの`onWindowPresented()`メソッドを呼び出して表示イベントを通知
- `dismiss(withDelay: Boolean)`: ウィンドウを非表示にする
  - 遅延フラグが設定されている場合は、50ミリ秒後に非表示処理を実行
  - 親クラスの`dismiss()`メソッドを呼び出してウィンドウを非表示に
  - すべての子ビューを削除してリソースを解放
  - デリゲートの`onWindowDismissed()`メソッドを呼び出して非表示イベントを通知

#### 使用例

IAMWindowは内部クラスであり、直接アプリ開発者が使用することはありませんが、以下のようにIAMProcessorクラス内部で使用されます：

```kotlin
// IAMProcessorクラス内での使用例（実際の実装の簡略版）
internal class IAMProcessor(application: Application, private val panelWindowManager: PanelWindowManager) : ActivityLifecycleCallback(), WebViewDelegate {
    private var window: IAMWindow? = null
    private var currentActivity: WeakReference<Activity>? = null
    private var isWindowFocus = false
    
    // ウィンドウの表示処理
    private fun show(activity: Activity) {
        if (window?.activity == activity) {
            window?.show() // 同じアクティビティの場合は単純に表示
            return
        }
        dismiss() // 異なるアクティビティの場合は一旦非表示にしてから再作成
        
        // 新しいウィンドウを作成して表示
        window = IAMWindow(activity, panelWindowManager)
        window?.show(isWindowFocus, webView)
    }
    
    // ウィンドウの非表示処理
    private fun dismiss(withDelay: Boolean = false) {
        window?.dismiss(withDelay) // 遅延フラグを指定して非表示
        window = null // 参照をクリア
    }
    
    // WebViewDelegateの実装
    override fun onWebViewVisible() {
        currentActivity?.get()?.let {
            show(it) // WebViewが可視になったらウィンドウを表示
        }
    }
    
    override fun onWebViewInvisible() {
        dismiss() // WebViewが不可視になったらウィンドウを非表示
    }
    
    // アクティビティライフサイクルの処理
    override fun onActivityPaused(activity: Activity) {
        // アクティビティが一時停止する際に遅延付きで非表示
        // （リセット防止フラグがない場合）
        if (!ResetPrevent.isPreventReset(activity)) {
            reset(false)
            dismiss(true) // 遅延付きで非表示
        }
        currentActivity = null
    }
}
```

内部的には、IAMWindowはWebViewコンテンツを表示するためのコンテナとして機能し、ウィンドウの表示・非表示のタイミングでデリゲートに通知します。これにより、アプリ開発者はInAppMessagingDelegateを通じてウィンドウの表示・非表示イベントをキャッチし、適切な処理を行うことができます。

また、遅延ディスミス機能は、アクティビティのライフサイクルイベント中（特に`onPause`中）にウィンドウを安全に非表示にするために重要です。これにより、アクティビティの状態変化中に発生する可能性のあるウィンドウ操作の問題を回避しています。

### MessageModel

`MessageModel`クラスはKARTEプラットフォームから受信したアプリ内メッセージのデータを解析し、管理するためのモデルクラスです。メッセージの表示条件の判定や、ページ遷移時のフィルタリングなどの機能を提供します。

#### クラスの目的と責任
- KARTEプラットフォームから受信したメッセージデータの解析と管理
- メッセージの表示条件の判定（ロード、フォーカス、クロス表示など）
- ページ遷移時のメッセージフィルタリング
- メッセージデータのBase64エンコード
- メッセージの文字列表現の提供

#### 技術的な詳細
- JSONObjectとJSONArrayを使用してメッセージデータを解析・管理
- Base64エンコーディングを使用してメッセージデータを文字列化
- 例外処理を実装し、JSONの解析エラーやメモリ不足などの問題に対応
- ページビューID（pvId）に基づくメッセージフィルタリングロジックを実装
- キャンペーン設定（`native_app_display_limit_mode`、`native_app_window_focusable`、`native_app_cross_display_mode`など）に基づく表示制御
- リモートコンフィグタイプのメッセージを識別し、表示対象から除外
- メッセージの詳細情報を含むデバッグ用の文字列表現を提供

#### メンバー変数
- `data`: メッセージデータを保持するJSONObject（サーバーからのレスポンスデータ）
- `request`: 関連するトラッキングリクエスト（ページビューIDなどの情報を含む）
- `string`: Base64エンコードされたメッセージデータ文字列（WebViewに渡すためのデータ）
- `messages`: メッセージのリスト（JSONObjectのリストとして取得可能）

#### メソッド
- `filter(pvId: String, exclude: (JSONObject, String) -> Unit)`: ページビューIDに基づいてメッセージをフィルタリングする
  - 現在のページビューIDと異なる場合に、`native_app_display_limit_mode`が有効なメッセージを除外
  - 除外されたメッセージは`exclude`コールバックを通じて通知
- `shouldLoad()`: メッセージをロードすべきかどうかを判断する
  - リモートコンフィグタイプ以外のメッセージが含まれている場合に`true`を返す
- `shouldFocus()`: ウィンドウにフォーカスを与えるべきかどうかを判断する
  - `native_app_window_focusable`が`true`のメッセージが含まれている場合に`true`を返す
- `shouldFocusCrossDisplayCampaign()`: クロス表示キャンペーンにフォーカスを与えるべきかどうかを判断する
  - `native_app_window_focusable`と`native_app_cross_display_mode`の両方が`true`のメッセージが含まれている場合に`true`を返す
- `toString()`: メッセージの文字列表現を返す（デバッグ用）
  - メッセージのID、短縮ID、キャンペーン情報などを含む

#### 使用例

MessageModelは内部クラスであり、直接アプリ開発者が使用することはありませんが、以下のようにInAppMessagingシステム内部で使用されます：

```kotlin
// InAppMessagingクラス内での使用例（実際の実装の簡略版）
internal class InAppMessaging : Library, ActionModule, UserModule, TrackModule {
    // トラッキングレスポンスの処理
    override fun receive(trackResponse: TrackResponse, trackRequest: TrackRequest) {
        val responseData = trackResponse.responseData ?: return
        
        try {
            // MessageModelの作成
            val messageModel = MessageModel(responseData, trackRequest)
            
            // 現在のページビューIDに基づくフィルタリング
            messageModel.filter(pvId) { message, reason ->
                // 除外されたメッセージのトラッキング
                trackMessageSuppressed(message, reason)
            }
            
            // メッセージをロードすべきか判定
            if (messageModel.shouldLoad()) {
                // メッセージの処理
                processor.handle(messageModel)
            }
        } catch (e: JSONException) {
            Logger.e(LOG_TAG, "Failed to parse message data", e)
        }
    }
}

// IAMProcessorクラス内での使用例（実際の実装の簡略版）
internal class IAMProcessor(application: Application, private val panelWindowManager: PanelWindowManager) {
    // メッセージモデルの処理
    fun handle(message: MessageModel) {
        // フォーカス状態の設定
        isWindowFocus = if (isWindowFocusByCross) {
            true // クロス表示キャンペーンの場合は常にフォーカスを維持
        } else {
            message.shouldFocus() // 通常のメッセージの場合はフォーカス設定に従う
        }
        
        // WebViewにデータを渡す
        webView?.handleResponseData(message.string)
    }
}
```

内部的には、MessageModelはサーバーから受信したJSONデータを解析し、メッセージの表示条件を判定する重要な役割を果たします。特に、ページ遷移時のメッセージフィルタリングや、フォーカス状態の判定は、アプリ内メッセージの適切な表示制御に不可欠です。

また、`native_app_display_limit_mode`や`native_app_cross_display_mode`などの設定に基づいて、メッセージの表示方法を制御することで、ユーザー体験を向上させています。例えば、クロス表示モードが有効なメッセージは、ページ遷移後も表示を維持することができます。

### PanelWindowManager

`PanelWindowManager`クラスはアプリ内メッセージとアプリ自身のウィンドウ間のタッチイベント調整を行う重要なクラスです。複数のウィンドウが重なって表示されている場合に、ユーザーのタッチ操作が適切なウィンドウに届くように制御します。

#### クラスの目的と責任
- アプリが表示している`PopupWindow`と`Window`（特に`TYPE_APPLICATION_PANEL`タイプ）を登録・管理する
- アプリ内メッセージ表示中に、登録されたウィンドウへのタッチイベントを適切に分配する
- アプリ内メッセージとアプリのポップアップウィンドウが同時に表示されている場合のタッチイベントの衝突を解決する
- ウィンドウの表示状態やタッチ可能状態に基づいてイベントをフィルタリングする
- タッチイベントの連続性（ドラッグ操作など）を保証する
- 無効になったウィンドウ参照を自動的に削除し、メモリリークを防止する

#### 技術的な詳細
- 弱参照（`WeakReference`）を使用してメモリリークを防止しながらウィンドウを管理
  - `Window`と`PopupWindow`への参照を弱参照として保持し、ガベージコレクションを妨げない
  - 参照が無効になった場合は自動的にリストから削除する機能を実装
- タッチイベントの座標変換
  - 画面座標系からウィンドウ座標系への変換を行い、正確なイベント配信を実現
  - `getLocationOnScreen`と`offsetLocation`を使用して座標変換を実装
- ACTION_DOWNイベントの追跡
  - タッチシーケンスの開始（ACTION_DOWN）を記録し、後続のイベント（MOVE, UP）を同じウィンドウに配信
  - `lastActionDownWindow`変数を使用してタッチシーケンスの一貫性を保証
- ウィンドウの状態チェック
  - `isActivePanel`メソッドを使用してウィンドウが有効か（表示中か、適切なタイプか）を確認
  - `visibility`と`isAttachedToWindow`の両方をチェックして正確な表示状態を判定
- 優先順位ベースの処理
  - 最近登録されたウィンドウから順にイベント処理を試み、最初に成功したウィンドウでイベント処理を完了
  - リストの先頭に新しいウィンドウを追加することで優先順位を実現
- Androidバージョン互換性対応
  - `@RequiresApi(api = Build.VERSION_CODES.KITKAT)`アノテーションを使用してAPI 19以上での動作を保証
  - API 23（Marshmallow）以上での`windowLayoutType`チェックを実装

#### 内部クラス
- `BaseWindowWrapper`: ウィンドウラッパーの基本インターフェース
  - `hasStaleReference()`: 参照が無効になったかどうかを確認
  - `dispatchTouch(event: MotionEvent)`: タッチイベントの配信を試みる（成功した場合はtrueを返す）
  - `dispatchTouchForce(event: MotionEvent)`: 強制的にタッチイベントを配信（チェックをスキップ）

- `WindowWrapper`: Androidの`Window`クラスのラッパー実装
  - `Window`への弱参照を保持し、メモリリークを防止
  - `isActivePanel`メソッドでウィンドウの状態を確認（TYPE_APPLICATION_PANELタイプか、表示中か、アタッチされているか）
  - ウィンドウのフラグ（`FLAG_NOT_TOUCHABLE`、`FLAG_NOT_TOUCH_MODAL`）に基づいてイベント処理を決定
  - `injectInputEvent`メソッドを使用してウィンドウにイベントを直接送信

- `PopupWindowWrapper`: Androidの`PopupWindow`クラスのラッパー実装
  - `PopupWindow`への弱参照を保持し、メモリリークを防止
  - `isActivePanel`メソッドでポップアップの状態を確認（表示中か、コンテンツビューがあるか、適切なタイプか）
  - `isTouchable`と`isOutsideTouchable`の設定に基づいてイベント処理を決定
  - `dispatchTouchEvent`メソッドを使用してルートビューにイベントを送信

#### メンバー変数
- `lastActionDownWindow`: 最後に`ACTION_DOWN`イベントを受け取ったウィンドウのラッパー（タッチシーケンスの追跡用）
- `windows`: 登録されたウィンドウラッパーのリスト（最近登録されたものが先頭に配置される）

#### メソッド
- `registerPopupWindow(popupWindow: PopupWindow)`: ポップアップウィンドウを登録する
  - 登録されたポップアップウィンドウはリストの先頭に追加され、タッチイベント処理の優先順位が高くなる
  - `PopupWindowWrapper`でラップしてリストに追加
- `registerWindow(window: Window)`: ウィンドウを登録する
  - 登録されたウィンドウはリストの先頭に追加され、タッチイベント処理の優先順位が高くなる
  - `WindowWrapper`でラップしてリストに追加
- `dispatchTouch(event: MotionEvent)`: タッチイベントを適切なウィンドウに配信する
  - `ACTION_DOWN`イベントの場合：登録されたすべてのウィンドウを順に試し、イベントを処理できるウィンドウを探す
  - その他のイベントの場合：最後に`ACTION_DOWN`を処理したウィンドウに配信
  - 無効な参照を持つウィンドウは自動的にリストから削除
  - イベントが処理された場合は`true`を返し、処理されなかった場合は`false`を返す

#### タッチイベント処理の詳細
- `WindowWrapper`の場合：
  - ウィンドウが`TYPE_APPLICATION_PANEL`タイプであることを確認
  - ウィンドウが表示中で、アタッチされていることを確認
  - `FLAG_NOT_TOUCHABLE`フラグがないことを確認
  - `FLAG_NOT_TOUCH_MODAL`フラグがある場合は常にイベントを配信
  - そうでない場合は、タッチ座標がウィンドウ内にあるかを確認
  - 座標変換を行ってから`injectInputEvent`でイベントを配信
- `PopupWindowWrapper`の場合：
  - ポップアップウィンドウが表示中で、コンテンツビューがあることを確認
  - API 23以上では、ウィンドウタイプが`TYPE_APPLICATION_PANEL`であることを確認
  - `isTouchable`がtrueであることを確認
  - `isOutsideTouchable`がtrueの場合は常にイベントを配信
  - そうでない場合は、タッチ座標がポップアップウィンドウ内にあるかを確認
  - 座標変換を行ってから`dispatchTouchEvent`でイベントを配信

#### 使用例

アプリ内メッセージが表示されている状態で、アプリ自身もポップアップウィンドウやダイアログを表示している場合、`PanelWindowManager`はユーザーのタッチ操作が適切なウィンドウに届くように調整します。これにより、アプリ内メッセージとアプリのUIが同時に表示されていても、ユーザー体験を損なわずに操作できます。

```kotlin
// IAMProcessorクラス内での使用例（実際の実装の簡略版）
internal class IAMProcessor(application: Application, private val panelWindowManager: PanelWindowManager) {
    private var window: IAMWindow? = null
    
    // アクティビティが作成されたときの処理
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        // アプリのウィンドウを登録
        panelWindowManager.registerWindow(activity.window)
    }
    
    // ダイアログが表示されたときの処理
    fun onDialogShown(dialog: Dialog) {
        // ダイアログのウィンドウを登録
        panelWindowManager.registerWindow(dialog.window!!)
    }
    
    // ポップアップが表示されたときの処理
    fun onPopupWindowShown(popupWindow: PopupWindow) {
        // ポップアップウィンドウを登録
        panelWindowManager.registerPopupWindow(popupWindow)
    }
    
    // IAMWindowの作成と表示
    private fun show(activity: Activity) {
        // 新しいウィンドウを作成
        window = IAMWindow(activity, panelWindowManager)
        window?.show(isWindowFocus, webView)
    }
}

// WindowViewクラス内での使用例（実際の実装の簡略版）
internal open class WindowView(context: Context, private val panelWindowManager: PanelWindowManager) : FrameLayout(context) {
    // タッチイベントの処理
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        // PanelWindowManagerにタッチイベントを委譲
        if (panelWindowManager.dispatchTouch(event)) {
            return true
        }
        return super.dispatchTouchEvent(event)
    }
}
```

実際の使用シーンでは、以下のようなケースでPanelWindowManagerが活躍します：

1. **複数ウィンドウの共存**：アプリ内メッセージが表示されている状態で、アプリがダイアログを表示した場合、ユーザーは両方のUIを操作できます。

2. **ドラッグ操作の一貫性**：ユーザーがスライダーやスクロール可能な要素をドラッグしている場合、ACTION_DOWNから始まるすべてのイベントが同じウィンドウに配信されるため、操作が途中で別のウィンドウに移ることがありません。

3. **優先順位ベースの処理**：最近表示されたウィンドウ（ダイアログなど）が優先的にタッチイベントを受け取るため、ユーザーの意図に沿った動作が実現されます。

4. **メモリリーク防止**：ウィンドウが閉じられた後も参照が残ることによるメモリリークを防止し、長時間の使用でもアプリのパフォーマンスを維持します。

このように、PanelWindowManagerはアプリ内メッセージとアプリのUIの共存を可能にし、シームレスなユーザー体験を提供する重要な役割を果たしています。

### ResetPrevent

`ResetPrevent`クラスは、アプリ内メッセージの特定操作中（ファイル選択やURL開封など）にアクティビティの遷移が発生した場合でも、メッセージの状態がリセットされることを防止するための内部ユーティリティクラスです。これにより、ユーザー体験の一貫性を保ち、操作の中断を防止します。

#### クラスの目的と責任
- アクティビティの遷移時にアプリ内メッセージの状態リセットを防止する
- ファイル選択やURL開封などの操作中にアプリ内メッセージが消えるのを防ぐ
- アクティビティのIntentにフラグを設定・取得することでリセット防止状態を管理する
- 一時的なリセット防止状態を適切に解除する（フラグの自動クリーンアップ）
- 複数のコンポーネント間で一貫したリセット防止メカニズムを提供する

#### 技術的な詳細
- Kotlinの`object`シングルトンパターンを使用した効率的なユーティリティクラス実装
- Androidの`Intent`エクストラメカニズムを活用したアクティビティ間のフラグ伝達
- `PREVENT_RESET_KEY`定数を使用した一貫性のあるキー名管理
- フラグ取得後の自動クリーンアップによるメモリリーク防止と状態の一貫性確保
- nullセーフな実装による堅牢性の確保（`activity?.intent?.putExtra`、`activity.intent?.let`）
- 内部APIとしての設計（`internal`修飾子）によるカプセル化

#### 静的メンバー
- `PREVENT_RESET_KEY`: リセット防止フラグをIntentに保存する際に使用するキー名（"krt_iam_prevent_reset"）

#### メソッド
- `enablePreventResetFlag(activity: Activity?)`: 指定されたアクティビティのIntentにリセット防止フラグを設定する
  - `activity`: フラグを設定するアクティビティ（nullの場合は何もしない）
  - 実装: `activity?.intent?.putExtra(PREVENT_RESET_KEY, true)`

- `isPreventReset(activity: Activity): Boolean`: 指定されたアクティビティのIntentからリセット防止フラグを取得し、同時にフラグをクリアする
  - `activity`: フラグを確認するアクティビティ
  - 戻り値: リセットを防止すべき場合は`true`、そうでない場合は`false`
  - 実装: Intentからフラグを取得し、同時に`removeExtra`でフラグをクリア

#### 使用例

このクラスは内部クラスであり、直接アプリ開発者が使用することはありませんが、以下のようにSDK内部で使用されます：

```kotlin
// IAMProcessorクラス内での使用例（実際の実装の簡略版）
internal class IAMProcessor(private val delegate: InAppMessagingDelegate) : ActivityLifecycleCallback(), WebViewDelegate {
    // URLを開く処理
    override fun onOpenUrl(uri: Uri, withReset: Boolean) {
        // リセット防止が必要な場合（withReset = false）
        if (!withReset) {
            // 現在のアクティビティにリセット防止フラグを設定
            currentActivity?.get().let { ResetPrevent.enablePreventResetFlag(it) }
        }
        
        // URLを開く処理...
    }
    
    // アクティビティ一時停止時の処理
    override fun onActivityPaused(activity: Activity) {
        // リセット防止フラグをチェック（同時にフラグはクリアされる）
        val isPreventReset = ResetPrevent.isPreventReset(activity)
        
        // リセット防止フラグがない場合のみリセット処理を実行
        if (!isPreventReset) {
            reset(false)
        }
    }
}

// FileChooserFragmentクラス内での使用例
class FileChooserFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // ファイル選択ダイアログを作成
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        
        // ファイル選択中にアプリ内メッセージがリセットされないようにフラグを設定
        ResetPrevent.enablePreventResetFlag(activity)
        
        // ファイル選択ダイアログを表示...
    }
}
```

実際の動作フローは以下のようになります：

1. **ファイル選択の場合**:
   - ユーザーがアプリ内メッセージ内のファイル選択ボタンをタップ
   - `FileChooserFragment`が表示される
   - フラグメント表示前に`ResetPrevent.enablePreventResetFlag(activity)`が呼ばれる
   - ユーザーがファイル選択画面に移動（アクティビティ遷移が発生）
   - `onActivityPaused`が呼ばれるが、`ResetPrevent.isPreventReset(activity)`が`true`を返すため、メッセージはリセットされない
   - ユーザーがファイルを選択して元の画面に戻る
   - アプリ内メッセージは引き続き表示されたまま

2. **URL開封の場合**:
   - ユーザーがアプリ内メッセージ内のリンクをタップ
   - `onOpenUrl`が`withReset = false`で呼ばれる
   - `ResetPrevent.enablePreventResetFlag(activity)`が呼ばれる
   - ブラウザやアプリ内WebViewでURLが開かれる
   - アクティビティ遷移時に`onActivityPaused`が呼ばれるが、リセットは防止される
   - ユーザーが元の画面に戻ると、アプリ内メッセージは引き続き表示されている

このメカニズムにより、ユーザーがファイル選択やリンクタップなどの操作を行っても、アプリ内メッセージの状態が維持され、一貫したユーザー体験が提供されます。例えば、ユーザーがアプリ内メッセージからギャラリーアプリを開いて画像を選択し、元のアプリに戻ってきた際に、アプリ内メッセージが消えずに表示され続けるため、ユーザーは中断することなく操作を完了できます。

## JavaScript関連

### JsMessage

`JsMessage`クラスはWebViewとネイティブコード間のJavaScriptメッセージを表現するためのシールドクラスです。異なるタイプのメッセージを型安全に処理するための構造を提供します。

#### クラスの目的と責任
- WebViewのJavaScriptからのメッセージを型安全に表現する
- メッセージタイプに応じた適切なデータ構造を提供する
- JSON形式のメッセージデータを解析し、適切なJsMessageサブクラスに変換する
- メッセージの種類に基づいて異なる処理を可能にする
- WebViewとネイティブコード間の通信プロトコルを定義する

#### 技術的な詳細
- Kotlinのシールドクラス（`sealed class`）を使用して型安全なメッセージ階層を実現
  - コンパイル時の型チェックにより、すべてのメッセージタイプを網羅的に処理することを保証
  - `when`式での網羅性チェックを活用した安全なメッセージハンドリング
- JSONObjectとJSONArrayを使用してJavaScriptからのデータを解析
- Uriクラスを使用してURL文字列を適切に処理
- 例外処理を実装し、不正なメッセージデータに対する堅牢性を確保
- コンパニオンオブジェクトを使用して静的なパース機能を提供
- 定数を使用してメッセージタイプを定義し、一貫性を保証

#### 継承関係
- 抽象基底クラス`JsMessage`を継承した複数のメッセージタイプクラスを提供
- 各サブクラスは特定のメッセージタイプに必要なデータと機能を実装

#### メンバー変数
- `name`: メッセージの種類を示す文字列（各サブクラスで実装）

#### 内部クラス（サブクラス）
- `Event`: イベントメッセージを表現
  - `eventName`: イベント名（例：「campaign_open」、「campaign_close」など）
  - `values`: イベントに関連する値（JSONObject形式）
- `StateChanged`: WebViewの状態変更メッセージを表現
  - `state`: 新しい状態（例：「ready」、「loading」など）
- `OpenUrl`: URL開封メッセージを表現
  - `uri`: 開くべきURL（Uriオブジェクト）
  - `withReset`: URLを開いた後にWebViewをリセットすべきかどうか（targetが「_blank」でない場合はtrue）
- `Visibility`: WebViewの可視性変更メッセージを表現
  - `visible`: WebViewが可視かどうかを示すブール値
- `DocumentChanged`: ドキュメント変更（タッチ可能領域の更新）メッセージを表現
  - `regions`: タッチ可能な領域のリスト（JSONArray形式）

#### メソッド
- `parse(name: String, data: String)`: メッセージ名とJSONデータ文字列からJsMessageインスタンスを生成する
  - メッセージタイプに応じて適切なサブクラスのインスタンスを返す
  - 不正なデータや未知のメッセージタイプの場合はnullを返す

#### 使用例

JsMessageは内部クラスであり、直接アプリ開発者が使用することはありませんが、以下のようにIAMWebViewクラス内部で使用されます：

```kotlin
// IAMWebViewクラス内での使用例（実際の実装の簡略版）
internal class IAMWebView(context: Context, val delegate: WebViewDelegate) : BaseWebView(context) {
    // JavaScriptからのメッセージを受信するメソッド
    @JavascriptInterface
    fun onReceivedMessage(name: String, data: String) {
        uiThreadHandler.post { handleJsMessage(name, data) }
    }
    
    // JavaScriptメッセージの処理
    private fun handleJsMessage(name: String, data: String) {
        // JsMessage.parseを使用してメッセージを解析
        when (val message = JsMessage.parse(name, data)) {
            // イベントメッセージの処理
            is JsMessage.Event -> {
                // イベントをトラッキングシステムに送信
                Tracker.track(message.eventName, message.values)
                
                // キャンペーンの開閉イベントの場合はデリゲートに通知
                if (message.eventName == "campaign_open" || message.eventName == "campaign_close") {
                    notifyCampaignOpenOrClose(message.eventName, message.values)
                }
            }
            
            // 状態変更メッセージの処理
            is JsMessage.StateChanged -> {
                // WebViewの状態を更新
                changeState(State.of(message.state))
            }
            
            // URL開封メッセージの処理
            is JsMessage.OpenUrl -> {
                // URLを開く処理を実行
                tryOpenUrl(message.uri, message.withReset)
            }
            
            // 可視性変更メッセージの処理
            is JsMessage.Visibility -> {
                // WebViewの可視性を更新
                visible = message.visible
                
                // デリゲートに通知
                if (message.visible) {
                    delegate.onWebViewVisible()
                } else {
                    delegate.onWebViewInvisible()
                }
            }
            
            // ドキュメント変更メッセージの処理
            is JsMessage.DocumentChanged -> {
                // タッチ可能領域を更新
                delegate.updateTouchableRegions(message.regions)
            }
            
            // nullの場合（不正なメッセージや未知のメッセージタイプ）
            null -> {
                Logger.w(LOG_TAG, "Unknown message: $name")
            }
        }
    }
}
```

内部的には、JsMessageはWebViewとネイティブコード間の通信を型安全に行うための重要な役割を果たします。JavaScriptから送信されるJSONデータを適切なKotlinオブジェクトに変換し、メッセージタイプに応じた処理を可能にします。

例えば、アプリ内メッセージ内のボタンがクリックされると、JavaScriptから「event」タイプのメッセージが送信され、JsMessage.Eventオブジェクトに変換されます。このオブジェクトには、イベント名（例：「button_click」）と関連する値（ボタンのIDなど）が含まれ、適切なトラッキングイベントとして処理されます。

また、メッセージの表示状態が変わると、JavaScriptから「visibility」タイプのメッセージが送信され、JsMessage.Visibilityオブジェクトに変換されます。これにより、ネイティブ側でメッセージの表示・非表示を適切に処理できます。

このように、JsMessageはWebViewとネイティブコード間の通信プロトコルを定義し、型安全な方法でメッセージを処理するための基盤を提供しています。

### State

`State`列挙型はWebViewの状態を表現するための列挙型クラスです。アプリ内メッセージを表示するWebViewのライフサイクル状態を管理し、状態に応じた適切な処理を行うために使用されます。

#### クラスの目的と責任
- WebViewの現在の状態を表現する
- JavaScriptからのコールバック名を適切な状態列挙値に変換する
- WebViewの状態遷移を型安全に管理する
- 状態に基づいた条件分岐を可能にする
- WebViewの準備状態を追跡し、適切なタイミングでメッセージを処理する

#### 技術的な詳細
- Kotlinの列挙型（`enum class`）を使用して型安全な状態管理を実現
- コンパニオンオブジェクトを使用して静的なヘルパーメソッドを提供
- JavaScriptからのコールバック名と内部状態の対応関係を定義
  - `"initialized"` → `READY`
  - `"error"` → `DESTROYED`
- 不正な状態名に対しては`IllegalArgumentException`をスローし、早期エラー検出を実現
- 定数を使用してJavaScriptからのコールバック名を定義し、一貫性を保証
- 列挙型の特性を活用した網羅的な状態チェックを実現

#### 列挙値
- `LOADING`: WebViewがロード中または初期化中の状態
  - この状態では、WebViewはまだJavaScriptとの通信準備ができていない
  - メッセージデータはキューに保存され、READY状態になった後に処理される
- `READY`: WebViewが初期化を完了し、メッセージを表示する準備ができた状態
  - JavaScriptからの`"initialized"`コールバックを受信すると、この状態に遷移
  - この状態では、WebViewはメッセージの表示やイベントの処理が可能
  - キューに保存されたメッセージデータがこの状態で処理される
- `DESTROYED`: WebViewがエラー状態または破棄された状態
  - JavaScriptからの`"error"`コールバックを受信すると、この状態に遷移
  - この状態では、WebViewは再初期化が必要
  - エラーハンドリングやリソース解放のトリガーとして使用される

#### メソッド
- `of(nameInCallback: String)`: JavaScriptからのコールバック名を適切な`State`列挙値に変換する
  - `"initialized"`の場合は`READY`を返す
  - `"error"`の場合は`DESTROYED`を返す
  - それ以外の場合は`IllegalArgumentException`をスロー

#### 使用例

Stateは内部クラスであり、直接アプリ開発者が使用することはありませんが、以下のようにIAMWebViewクラス内部で使用されます：

```kotlin
// IAMWebViewクラス内での使用例（実際の実装の簡略版）
internal class IAMWebView(context: Context, val delegate: WebViewDelegate) : BaseWebView(context) {
    // WebViewの状態
    private var state: State = State.LOADING
    
    // 処理待ちのデータキュー
    private val queue = ArrayList<Data>()
    
    // WebViewが準備完了状態かどうか
    val isReady: Boolean
        get() = state == State.READY
    
    // JavaScriptからの状態変更メッセージを処理
    private fun handleJsMessage(name: String, data: String) {
        when (val message = JsMessage.parse(name, data)) {
            // 状態変更メッセージの処理
            is JsMessage.StateChanged -> {
                try {
                    // JavaScriptからの状態名をStateに変換
                    val newState = State.of(message.state)
                    // 状態を変更
                    changeState(newState)
                } catch (e: IllegalArgumentException) {
                    Logger.e(LOG_TAG, e.message ?: "")
                }
            }
            // その他のメッセージ処理...
        }
    }
    
    // 状態変更処理
    private fun changeState(newState: State) {
        Logger.d(LOG_TAG, "State changed: $state -> $newState")
        state = newState
        
        when (newState) {
            State.READY -> {
                // READY状態になったらキューに溜まったデータを処理
                queue.forEach { data ->
                    when (data) {
                        is ResponseData -> handleResponseData(data.json)
                        is ViewData -> handleView(data.values)
                    }
                }
                queue.clear()
            }
            State.DESTROYED -> {
                // DESTROYED状態になったらエラー処理
                errorOccurred()
            }
            State.LOADING -> {
                // LOADING状態の処理（必要に応じて）
            }
        }
    }
    
    // メッセージデータの処理
    fun handleResponseData(data: String) {
        if (isReady) {
            // 準備完了状態ならすぐに処理
            evaluateJavascript("window.IAM.handleResponseData('$data');", null)
        } else {
            // そうでなければキューに追加
            queue.add(ResponseData(data))
        }
    }
}
```

内部的には、Stateは以下のような役割を果たします：

1. **状態管理**: WebViewの現在の状態を追跡し、適切なタイミングでメッセージを処理します。例えば、WebViewがまだ準備完了していない（LOADING状態）場合、メッセージデータはキューに保存され、READY状態になった後に処理されます。

2. **エラー処理**: WebViewがエラー状態（DESTROYED）になった場合、適切なエラー処理を行います。例えば、エラーをログに記録したり、デリゲートに通知したりします。

3. **条件分岐**: 状態に基づいた条件分岐を可能にします。例えば、`isReady`プロパティを使用して、WebViewが準備完了状態かどうかを簡単に確認できます。

4. **状態変換**: JavaScriptからのコールバック名を適切な状態列挙値に変換します。例えば、JavaScriptから`"initialized"`コールバックを受信すると、`READY`状態に変換されます。

このように、Stateはアプリ内メッセージングシステムにおいて、WebViewの状態を適切に管理し、状態に応じた処理を行うための重要な役割を果たしています。

## プレビュー関連

### PreviewParams

`PreviewParams`クラスはKARTEのアプリ内メッセージのプレビュー機能を実現するためのパラメータを管理するクラスです。アプリ開発者がKARTEダッシュボードで作成したアプリ内メッセージを、実際の配信前にアプリ上でプレビューするための機能を提供します。

#### クラスの目的と責任
- アプリのディープリンクからプレビュー用のURLパラメータを安全に抽出する
- プレビュー表示の条件を判定する
- プレビュー用のWebViewに読み込ませるURLを生成する
- プレビューモードであることをKARTEサーバーに伝えるためのパラメータを構築する
- プレビュー関連のエラーを適切にログに記録する

#### 技術的な詳細
- Androidの`Activity`からインテントデータとクエリパラメータを抽出
- 例外処理を実装し、パラメータ取得時のエラーに対する堅牢性を確保
- JSONObjectを使用してプレビューパラメータを構造化
- KARTEのベースURLとアプリキーを使用してプレビュー用URLを構築
- ビジターIDやアプリ情報などの必要なパラメータをURLに付加
- ログ機能を使用して問題発生時のデバッグ情報を提供
- URLエンコーディングを考慮したパラメータ処理

#### メンバー変数
- `shouldShowPreview`: `__krt_preview`クエリパラメータの値（プレビューモードを示す）
- `previewId`: `preview_id`クエリパラメータの値（プレビュー対象のメッセージID）
- `previewToken`: `preview_token`クエリパラメータの値（プレビュー認証用トークン）

#### メソッド
- `shouldShowPreview()`: プレビュー表示の条件を満たしているかを判定する
  - 必要なパラメータ（`__krt_preview`、`preview_id`、`preview_token`）がすべて存在する場合にtrueを返す
  - いずれかのパラメータが欠けている場合はfalseを返す
- `toJSON()`: プレビューパラメータをJSON形式に変換する（内部メソッド）
  - プレビューIDとトークンを含むJSONObjectを生成
  - JSONの生成に失敗した場合はnullを返す
- `generateUrl(app: KarteApp)`: プレビュー用のURLを生成する
  - KARTEのベースURL、アプリキー、ビジターID、アプリ情報などを含むURLを構築
  - プレビューパラメータをURLに付加
  - URL生成に必要な情報が不足している場合はnullを返す

#### 使用例

PreviewParamsは内部クラスであり、直接アプリ開発者が使用することはありませんが、以下のようにIAMProcessorクラス内部で使用されます：

```kotlin
// IAMProcessorクラス内での使用例（実際の実装の簡略版）
internal class IAMProcessor(private val app: KarteApp) {
    // アクティビティが作成されたときの処理
    fun onActivityCreated(activity: Activity) {
        // プレビューパラメータの取得
        val previewParams = PreviewParams(activity)
        
        // プレビュー表示条件を満たしているか確認
        if (previewParams.shouldShowPreview()) {
            // プレビュー用URLの生成
            val url = previewParams.generateUrl(app)
            
            if (url != null) {
                // プレビュー用URLをWebViewに読み込ませる
                webView.loadUrl(url)
                
                // プレビューモードであることをログに記録
                Logger.i(LOG_TAG, "Preview mode activated")
                
                // プレビュー表示のためのUIを準備
                preparePreviewUI(activity)
            } else {
                // URL生成に失敗した場合のエラーハンドリング
                Logger.e(LOG_TAG, "Failed to generate preview URL")
            }
        }
    }
    
    // プレビュー表示のためのUI準備
    private fun preparePreviewUI(activity: Activity) {
        // プレビュー表示用のウィンドウを作成
        val window = IAMWindow(activity)
        
        // プレビューモードであることを示すUIを追加（オプション）
        window.addPreviewIndicator()
        
        // ウィンドウを表示
        window.show()
    }
}
```

実際のアプリ開発の流れでは、以下のようなシナリオでPreviewParamsが使用されます：

1. アプリ開発者がKARTEダッシュボードでアプリ内メッセージを作成し、プレビューボタンをクリック
2. KARTEダッシュボードがプレビュー用のディープリンクURLを生成（例：`myapp://open?__krt_preview=true&preview_id=123&preview_token=abc123`）
3. 開発者がこのURLをモバイルデバイスで開く（QRコードスキャンなど）
4. URLがアプリを起動し、インテントデータとしてプレビューパラメータが渡される
5. アプリ内のKARTE SDKがPreviewParamsを使用してパラメータを抽出
6. プレビュー条件を満たしている場合、PreviewParamsがプレビュー用のURLを生成
7. 生成されたURLがWebViewに読み込まれ、プレビューメッセージが表示される

このように、PreviewParamsはKARTEのアプリ内メッセージのプレビュー機能を実現するための重要なコンポーネントとして機能しています。アプリ開発者はこのクラスを直接使用することはありませんが、KARTEダッシュボードからのプレビュー機能を利用する際に、内部的にこのクラスが使用されています。

## ビュー関連

### AlertDialogFragment

`AlertDialogFragment`クラスはアプリ内メッセージングモジュール内でシンプルなアラートダイアログを表示するための内部フラグメントクラスです。WebViewでのエラーや通知を簡単にユーザーに表示するために使用されます。

#### クラスの目的と責任
- アプリ内メッセージングモジュール内でのエラーや通知をユーザーに表示する
- シンプルなメッセージとOKボタンを持つアラートダイアログを構築する
- アクティビティのライフサイクルに合わせてダイアログの表示を管理する
- WebViewからのアラート要求を処理する
- ダイアログの表示と非表示を適切に制御する

#### 技術的な詳細
- Androidの`DialogFragment`を継承し、フラグメントのライフサイクル管理を活用
- `@file:Suppress("DEPRECATION")`アノテーションを使用して非推奨APIの使用警告を抑制
- `Bundle`を使用してフラグメントにメッセージデータを渡す
- `AlertDialog.Builder`を使用してダイアログUIを構築
- 静的な`show`メソッドを提供し、ダイアログの作成と表示を簡略化
- フラグメントタグ（`"krt_alert_dialog"`）を使用してダイアログを識別
- Androidの標準リソース（`android.R.string.ok`）を使用してOKボタンのテキストを表示
- `onCreateDialog`メソッドをオーバーライドしてダイアログの内容をカスタマイズ

#### 継承関係
- `DialogFragment`を継承：Androidのダイアログフラグメント機能を利用

#### メンバー変数
- 特になし（メッセージは`arguments`バンドルに保存）

#### メソッド
- `onCreateDialog(savedInstanceState: Bundle)`: ダイアログを作成するメソッド
  - `arguments`からメッセージを取得
  - `AlertDialog.Builder`を使用してダイアログを構築
  - OKボタンを追加（アクションなし）
  - 構築したダイアログを返す

#### 静的メソッド
- `show(activity: Activity, message: String)`: アラートダイアログを表示する
  - 新しい`AlertDialogFragment`インスタンスを作成
  - メッセージを`Bundle`に格納してフラグメントに設定
  - アクティビティのフラグメントマネージャーを使用してダイアログを表示

#### 使用例

AlertDialogFragmentは内部クラスであり、直接アプリ開発者が使用することはありませんが、以下のようにBaseWebViewクラス内部で使用されます：

```kotlin
// BaseWebViewクラス内での使用例（実際の実装の簡略版）
internal abstract class BaseWebView(context: Context) : WebView(context) {
    // WebViewからのアラート表示要求を処理
    fun showAlert(message: String) {
        // 現在のアクティビティを取得
        val activity = findActivity(context)
        
        if (activity != null) {
            // AlertDialogFragmentを使用してアラートを表示
            AlertDialogFragment.show(activity, message)
        } else {
            // アクティビティが取得できない場合はログに記録
            Logger.w(LOG_TAG, "Failed to show alert: $message")
        }
    }
    
    // WebChromeClientの実装
    private inner class WebChromeClientImpl : WebChromeClient() {
        // JavaScriptのalert()関数の処理
        override fun onJsAlert(
            view: WebView,
            url: String,
            message: String,
            result: JsResult
        ): Boolean {
            // AlertDialogFragmentを使用してJavaScriptからのアラートを表示
            showAlert(message)
            result.confirm()
            return true
        }
    }
}
```

また、IAMWebViewクラスでエラーが発生した場合にも使用されます：

```kotlin
// IAMWebViewクラス内での使用例（実際の実装の簡略版）
internal class IAMWebView(context: Context, val delegate: WebViewDelegate) : BaseWebView(context) {
    // エラー処理メソッド
    override fun errorOccurred() {
        super.errorOccurred()
        
        // エラーメッセージを表示
        val activity = findActivity(context)
        if (activity != null) {
            AlertDialogFragment.show(
                activity,
                "アプリ内メッセージの表示中にエラーが発生しました。"
            )
        }
        
        // エラー発生をデリゲートに通知
        delegate.onWebViewError()
    }
}
```

このように、AlertDialogFragmentはアプリ内メッセージングモジュール内で、WebViewでのエラーやJavaScriptからのアラート要求を処理するための簡易的なダイアログ表示機能を提供しています。ユーザーに対して重要な通知やエラーメッセージを表示する際に使用され、シンプルなインターフェースでダイアログの作成と表示を簡略化しています。

### BaseWebView

`BaseWebView`クラスはアプリ内メッセージングモジュールで使用されるWebViewの基本機能を提供する抽象クラスです。安全なWebView設定、エラーハンドリング、セーフエリア対応、ファイル選択など、アプリ内メッセージ表示に必要な共通機能を実装しています。

#### クラスの目的と責任
- アプリ内メッセージ表示に最適化されたWebView基盤を提供する
- JavaScript実行環境の安全な設定と管理を行う
- WebViewのエラーを適切に処理し、ユーザー体験を損なわないようにする
- ノッチやカットアウトなどのセーフエリアに対応し、メッセージの表示位置を調整する
- WebViewとネイティブコード間の通信を管理する
- ファイル選択やアラート表示などのユーザーインタラクションを処理する
- デバッグモードでのWebView検査機能を提供する
- 異なるAndroidバージョン間での互換性を確保する

#### 技術的な詳細
- `@SuppressLint("SetJavaScriptEnabled")`アノテーションを使用してJavaScript有効化の警告を抑制
- カスタムWebViewClientとWebChromeClientの実装によるイベント処理
- SSL証明書エラー、HTTP通信エラー、リソース読み込みエラーなどの包括的なエラーハンドリング
- Android Pからのノッチ対応（DisplayCutout）とセーフエリア計算
- Android APIレベルに応じた条件分岐による互換性確保
  - KitKat（API 19）以前のデータベースパス設定
  - Android Q（API 29）以降のダークモード対応
  - Android P（API 28）以降のディスプレイカットアウト対応
  - Android R（API 30）以降のディスプレイマネージャー対応
- WebViewデバッグ機能のデバッグビルドでの有効化
- バックボタンのカスタム処理によるWebViewナビゲーション制御
- 透明背景設定によるカスタムUIの実現
- スクロールバーの非表示化によるUI改善
- ファイルスキーム（file://）へのアクセス制限によるセキュリティ強化
- コンソールメッセージのログ出力によるデバッグ支援
- 密度非依存ピクセル（dp）とピクセル間の変換処理

#### 継承関係
- `WebView`を継承：Androidの標準WebView機能を拡張
- 抽象クラスとして設計され、具体的な実装は子クラス（IAMWebView）に委譲

#### 内部クラス
- `SafeInsets`: セーフエリアのインセット値を保持するデータクラス
  - `left`: 左側のセーフエリアインセット（dp単位）
  - `top`: 上部のセーフエリアインセット（dp単位）
  - `right`: 右側のセーフエリアインセット（dp単位）
  - `bottom`: 下部のセーフエリアインセット（dp単位）

#### メンバー変数
- `safeInsets`: 現在のセーフエリアインセット情報を保持する変数

#### メソッド
- `init`: WebViewの初期設定を行うイニシャライザ
  - JavaScriptの有効化
  - パスワード保存の無効化
  - DOMストレージとデータベースの有効化
  - 透明背景の設定
  - スクロールバーの非表示化
  - デバッグ設定
  - WebViewClientとWebChromeClientの設定
- `onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int)`: レイアウト時にセーフエリアインセットを適用
- `onAttachedToWindow()`: ウィンドウにアタッチされた時にセーフエリアインセットを取得
- `dispatchKeyEvent(event: KeyEvent)`: キーイベント（特にバックボタン）の処理
- `destroy()`: WebViewのリソース解放処理
- `handleError(message: String, urlTriedToLoad: String?)`: エラー発生時の共通処理
- `getSafeInsets()`: 現在のディスプレイのセーフエリアインセットを取得
- `isLocatedAtTopOfScreen()`: WebViewが画面上部に配置されているかを判定

#### 抽象メソッド（サブクラスで実装必須）
- `setSafeAreaInset(top: Int)`: セーフエリアのインセットを設定する
- `errorOccurred()`: エラー発生時の処理を行う
- `openUrl(uri: Uri)`: URLを開く
- `showAlert(message: String)`: アラートを表示する
- `showFileChooser(filePathCallback: ValueCallback<Array<Uri>>)`: ファイル選択ダイアログを表示する

#### 使用例

BaseWebViewは抽象クラスであり、直接インスタンス化することはできません。代わりに、IAMWebViewクラスがこれを継承して具体的な実装を提供します：

```kotlin
// IAMWebViewクラスでのBaseWebView継承例（実際の実装の簡略版）
internal class IAMWebView(context: Context, private val delegate: WebViewDelegate) : BaseWebView(context) {
    // セーフエリアインセットの設定
    override fun setSafeAreaInset(top: Int) {
        // JavaScriptを使用してCSSのセーフエリア変数を設定
        val script = "document.documentElement.style.setProperty('--safe-area-inset-top', '${top}px');"
        evaluateJavascript(script, null)
    }
    
    // エラー発生時の処理
    override fun errorOccurred() {
        // 状態を更新
        changeState(State.DESTROYED)
        
        // エラーメッセージを表示
        val activity = findActivity(context)
        if (activity != null) {
            AlertDialogFragment.show(
                activity,
                "アプリ内メッセージの表示中にエラーが発生しました。"
            )
        }
        
        // デリゲートにエラーを通知
        delegate.onWebViewError()
    }
    
    // URLを開く処理
    override fun openUrl(uri: Uri) {
        // デリゲートにURLオープンを委譲
        if (delegate.shouldOpenURL(uri)) {
            // 標準的なインテント処理でURLを開く
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Logger.e(LOG_TAG, "Failed to open URL: $uri", e)
                showAlert("URLを開けませんでした: $uri")
            }
        }
    }
    
    // アラート表示
    override fun showAlert(message: String) {
        val activity = findActivity(context)
        if (activity != null) {
            AlertDialogFragment.show(activity, message)
        } else {
            Logger.w(LOG_TAG, "Failed to show alert: $message")
        }
    }
    
    // ファイル選択ダイアログの表示
    override fun showFileChooser(filePathCallback: ValueCallback<Array<Uri>>): Boolean {
        val activity = findActivity(context)
        return if (activity != null) {
            FileChooserFragment.showFileChooser(activity, filePathCallback)
            true
        } else {
            Logger.w(LOG_TAG, "Failed to show file chooser")
            false
        }
    }
}
```

BaseWebViewクラスは、アプリ内メッセージングモジュールにおいて以下のような役割を果たします：

1. **安全なWebView環境の提供**：JavaScriptの有効化やセキュリティ設定など、アプリ内メッセージの表示に必要な基本設定を行います。

2. **エラーハンドリング**：WebViewで発生する様々なエラー（SSL証明書エラー、HTTP通信エラー、リソース読み込みエラーなど）を適切に処理し、ユーザー体験を損なわないようにします。

3. **デバイス互換性の確保**：異なるAndroidバージョンやデバイス特性（ノッチやカットアウトなど）に対応し、一貫した表示を実現します。

4. **ユーザーインタラクションの処理**：バックボタンの処理、アラート表示、ファイル選択など、ユーザーとのインタラクションを適切に処理します。

5. **デバッグ支援**：デバッグビルドでのWebViewデバッグ機能の有効化やコンソールメッセージのログ出力など、開発者のデバッグを支援します。

このように、BaseWebViewはアプリ内メッセージングモジュールの中核となるWebView機能を提供し、IAMWebViewなどの具体的な実装クラスに共通基盤を提供しています。

### FileChooserFragment

`FileChooserFragment`クラスはWebViewでのファイル選択機能を実現するためのフラグメントクラスです。アプリ内メッセージ内でファイルアップロード機能を使用する際に、ネイティブのファイル選択ダイアログを表示し、選択結果をWebViewに返す役割を担います。

#### クラスの目的と責任
- WebViewのファイル選択リクエストを処理する
- ネイティブのファイル選択ダイアログを表示する
- 選択されたファイルのURIをWebViewに返す
- AndroidXとレガシーAPIの両方に対応し、幅広いアプリとの互換性を確保する
- ファイル選択中にアプリのリセットを防止する
- フラグメントのライフサイクルを適切に管理し、メモリリークを防止する
- ファイル選択操作のキャンセルを適切に処理する

#### 技術的な詳細
- 2つの実装クラスを提供：
  - `FileChooserFragment`: AndroidX（androidx.fragment.app.Fragment）を使用した現代的な実装
  - `FileChooserDeprecatedFragment`: レガシーAPI（android.app.Fragment）を使用した互換性のための実装
- `@file:Suppress("DEPRECATION")`アノテーションを使用して非推奨APIの使用警告を抑制
- `@SuppressLint("ValidFragment")`アノテーションを使用して空のコンストラクタに関する警告を抑制
- `Intent.ACTION_GET_CONTENT`と`Intent.CATEGORY_OPENABLE`を使用してファイル選択インテントを構築
- `image/*`タイプフィルタを使用して画像ファイルのみを選択可能に
- `ResetPrevent.enablePreventResetFlag`を使用してファイル選択中のアプリリセットを防止
- コールバックパターンを使用して選択結果を非同期で処理
- フラグメントトランザクションを使用してフラグメントの追加と削除を管理
- `onResume`メソッドでファイル選択インテントを起動し、フラグメントの重複表示を防止
- `onActivityResult`メソッドで選択結果を処理し、コールバックに通知
- `onDetach`メソッドでリスナー参照をクリアし、メモリリークを防止
- `try-catch`ブロックを使用してAndroidXが利用できない環境での例外を処理

#### 継承関係
- `FileChooserFragment`: `androidx.fragment.app.Fragment`を継承
- `FileChooserDeprecatedFragment`: `android.app.Fragment`を継承

#### メンバー変数
- `listener`: ファイル選択結果を受け取るコールバック関数
- `activityStarted`: アクティビティが既に開始されたかを示すフラグ

#### メソッド
- `onResume()`: フラグメントが表示されたときにファイル選択インテントを起動
- `onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)`: ファイル選択結果を処理
- `onDetach()`: フラグメントがデタッチされたときにリソースをクリーンアップ
- `removeFragment()`: フラグメントを削除するヘルパーメソッド
- `newInstance()`: フラグメントのインスタンスを作成する静的ファクトリメソッド

#### 静的メソッド
- `showFileChooser(activity: Activity, filePathCallback: ValueCallback<Array<Uri>>)`: ファイル選択ダイアログを表示する
  - アクティビティの種類に応じて適切なフラグメント（AndroidXまたはレガシー）を選択
  - フラグメントにリスナーを設定
  - フラグメントトランザクションを使用してフラグメントを追加
  - 処理結果をブール値で返す（常にtrue）

#### 使用例

FileChooserFragmentは内部クラスであり、直接アプリ開発者が使用することはありませんが、以下のようにBaseWebViewクラス内部で使用されます：

```kotlin
// BaseWebViewクラス内での使用例（実際の実装の簡略版）
internal abstract class BaseWebView(context: Context) : WebView(context) {
    // WebChromeClientの実装
    private inner class WebChromeClientImpl : WebChromeClient() {
        // ファイル選択リクエストの処理
        override fun onShowFileChooser(
            webView: WebView,
            filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams
        ): Boolean {
            // FileChooserFragmentを使用してファイル選択ダイアログを表示
            return showFileChooser(filePathCallback)
        }
    }
    
    // ファイル選択ダイアログの表示
    abstract fun showFileChooser(filePathCallback: ValueCallback<Array<Uri>>): Boolean
}

// IAMWebViewクラスでの実装例
internal class IAMWebView(context: Context, val delegate: WebViewDelegate) : BaseWebView(context) {
    // ファイル選択ダイアログの表示
    override fun showFileChooser(filePathCallback: ValueCallback<Array<Uri>>): Boolean {
        val activity = findActivity(context)
        return if (activity != null) {
            // FileChooserFragmentを使用してファイル選択ダイアログを表示
            FileChooserFragment.showFileChooser(activity, filePathCallback)
        } else {
            // アクティビティが取得できない場合は失敗
            Logger.w(LOG_TAG, "Failed to show file chooser")
            false
        }
    }
}
```

実際の動作フローは以下のようになります：

1. WebView内でファイルアップロード機能が使用される（例：`<input type="file">`要素がクリックされる）
2. WebViewの`WebChromeClient.onShowFileChooser`メソッドが呼び出される
3. BaseWebViewの`showFileChooser`抽象メソッドが呼び出される
4. IAMWebViewの`showFileChooser`実装が呼び出される
5. `FileChooserFragment.showFileChooser`静的メソッドが呼び出される
6. アクティビティの種類に応じて適切なフラグメント（AndroidXまたはレガシー）が選択される
7. フラグメントがアクティビティに追加される
8. フラグメントの`onResume`メソッドでファイル選択インテントが起動される
9. ユーザーがファイルを選択するか、操作をキャンセルする
10. フラグメントの`onActivityResult`メソッドで選択結果が処理される
11. 結果がコールバックを通じてWebViewに返される
12. フラグメントが削除される

このように、FileChooserFragmentはWebViewとネイティブのファイル選択機能の橋渡しとして機能し、アプリ内メッセージ内でのファイルアップロード機能を実現しています。また、AndroidXとレガシーAPIの両方に対応することで、幅広いアプリとの互換性を確保しています。

### WindowView

`WindowView`クラスはアプリ内メッセージを表示するための特殊なオーバーレイビューです。アプリのコンテンツビュー上に重ねて表示され、タッチイベントの適切な振り分け、キーボード表示の管理、画面回転やシステムUIの変更への対応など、複雑なウィンドウ管理を担当します。

#### クラスの目的と責任
- アプリ内メッセージのコンテナとして機能し、WebViewを適切に配置する
- アプリのコンテンツビューと同じサイズ・位置に自動調整する
- タッチイベントをアプリとアプリ内メッセージの適切な対象に振り分ける
- キーボード表示時の適切なレイアウト調整を行う
- 画面回転やシステムUI（ステータスバー、ナビゲーションバーなど）の変更に対応する
- 異なるAndroidバージョン間での互換性を確保する
- フルスクリーンモードやマルチウィンドウモード（分割画面）での正しい表示を保証する
- バックボタンイベントの適切な処理を行う
- メモリリークを防止するためのリソース管理を行う

#### 技術的な詳細
- `FrameLayout`を継承し、`TYPE_APPLICATION_ATTACHED_DIALOG`タイプのウィンドウとして実装
- `ViewTreeObserver.OnGlobalLayoutListener`を実装してレイアウト変更を監視
- `WindowManager.LayoutParams`を使用したウィンドウパラメータの詳細な制御
- タッチイベントの処理に`Bitmap`と`Canvas`を使用した透明部分の検出
- `dispatchTouchEvent`のオーバーライドによるタッチイベントの振り分け
- `MotionEvent.obtain`と座標オフセット計算による正確なイベント転送
- `Window.injectInputEvent`を使用したアプリウィンドウへのイベント注入
- `InputMethodManager`を使用したソフトキーボードの制御
- `ResultReceiver`を使用したキーボード表示状態の非同期監視
- `WeakReference`を使用したメモリリーク防止
- Android APIレベルに応じた条件分岐による互換性確保
  - Android R（API 30）以降のWindowInsets API対応
  - Android P（API 28）以降のディスプレイカットアウト対応
  - Android N（API 24）以降の分割画面モード対応
- `WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN`などのフラグを使用したウィンドウ表示制御
- `PixelFormat.TRANSLUCENT`を使用した透明背景の実現
- JSONデータからのタッチ可能領域（RectF）の解析と管理
- デバッグビルド時の詳細なログ出力

#### 継承関係
- `FrameLayout`を継承：Androidの標準レイアウトコンテナを拡張
- `ViewTreeObserver.OnGlobalLayoutListener`を実装：レイアウト変更の監視

#### メンバー変数
- `appWindow`: アプリのウィンドウオブジェクト
- `windowManager`: ウィンドウマネージャー
- `isAttaching`: アタッチ処理中かどうかを示すフラグ
- `webViewDrawingBitmap`: WebViewの描画に使用するビットマップ
- `canvas`: WebViewの描画に使用するキャンバス
- `lastActionDownIsInClientApp`: 最後のタッチダウンイベントがアプリ領域内かどうかを示すフラグ
- `knownTouchableRegions`: 既知のタッチ可能領域のリスト
- `contentViewVisibleRect`: コンテンツビューの表示可能領域
- `iamViewVisibleRect`: IAMビューの表示可能領域
- `locationOnScreen`: 画面上の位置
- `contentViewLocationOnScreen`: コンテンツビューの画面上の位置
- `focusFlag`: フォーカス状態を示すフラグ
- `isStatusBarOverlaid`: ステータスバーがコンテンツビューに重なっているかどうか
- `isActivityNotRenewedOnRotate`: 画面回転時にアクティビティが再生成されないかどうか

#### メソッド
- `show()`: ビューを表示する
- `showInternal()`: ビュー表示の内部実装
- `dismiss()`: ビューを非表示にする
- `onAttachedToWindow()`: ウィンドウにアタッチされた時の処理
- `updateTouchableRegions(touchableRegions: JSONArray)`: タッチ可能な領域を更新する
- `parseJsonToRect(regionsJson: JSONArray)`: JSON配列からRectFのリストに変換する
- `onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int)`: レイアウト時の処理
- `setupBitmapAndCanvas()`: ビットマップとキャンバスの設定
- `dispatchTouchEvent(ev: MotionEvent)`: タッチイベントの振り分け
- `dispatchKeyEvent(event: KeyEvent)`: キーイベントの振り分け
- `setFocus(focus: Boolean)`: フォーカス状態の設定
- `touchIsInClientApp(ev: MotionEvent)`: タッチイベントがアプリ領域内かどうかを判定
- `hideAndShowKeyboard()`: キーボードを非表示にして再表示する
- `onGlobalLayout()`: グローバルレイアウト変更時の処理
- `onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int)`: サイズ測定時の処理
- `syncPadding()`: パディングの同期
- `updateRectAndLocation()`: 矩形と位置の更新
- `logWindowSize(message: String)`: ウィンドウサイズのログ出力

#### 使用例

WindowViewは内部クラスであり、直接アプリ開発者が使用することはありませんが、以下のようにIAMWindowクラス内部で使用されます：

```kotlin
// IAMWindowクラスでのWindowView使用例（実際の実装の簡略版）
internal class IAMWindow(
    activity: Activity,
    private val panelWindowManager: PanelWindowManager,
    private val delegate: IAMWindowDelegate
) {
    private val windowView: WindowView
    private val webView: IAMWebView
    
    init {
        // WindowViewのインスタンス化
        windowView = WindowView(activity, panelWindowManager)
        
        // WebViewの作成と追加
        webView = IAMWebView(activity, object : WebViewDelegate {
            override fun onWebViewClosed() {
                delegate.onWindowClosed()
            }
            
            override fun onWebViewError() {
                delegate.onWindowError()
            }
            
            override fun shouldOpenURL(uri: Uri): Boolean {
                return delegate.shouldOpenURL(uri)
            }
        })
        
        // WebViewをWindowViewに追加
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        windowView.addView(webView, params)
    }
    
    // アプリ内メッセージの表示
    fun show() {
        windowView.show()
    }
    
    // アプリ内メッセージの非表示
    fun dismiss() {
        windowView.dismiss()
    }
    
    // タッチ可能領域の更新
    fun updateTouchableRegions(touchableRegions: JSONArray) {
        windowView.updateTouchableRegions(touchableRegions)
    }
    
    // WebViewへのURLロード
    fun loadUrl(url: String) {
        webView.loadUrl(url)
    }
}
```

実際の動作フローは以下のようになります：

1. IAMProcessorがアプリ内メッセージの表示を要求する
2. IAMWindowが作成され、その中でWindowViewとIAMWebViewが初期化される
3. WindowViewがアプリのコンテンツビュー上にオーバーレイとして追加される
4. WindowViewはアプリのコンテンツビューと同じサイズ・位置に自動調整される
5. ユーザーがアプリ内メッセージの透明部分をタップすると、WindowViewがそのイベントをアプリに転送する
6. ユーザーがアプリ内メッセージの不透明部分をタップすると、WindowViewがそのイベントをWebViewに転送する
7. キーボードが表示されると、WindowViewが適切にレイアウトを調整する
8. 画面回転やシステムUIの変更が発生すると、WindowViewが自動的にサイズと位置を再調整する
9. バックボタンが押されると、WindowViewがそのイベントを適切に処理する

このように、WindowViewはアプリ内メッセージの表示基盤として機能し、複雑なウィンドウ管理やイベント処理を担当することで、アプリとアプリ内メッセージの共存を可能にしています。また、異なるAndroidバージョンやデバイス特性に対応することで、幅広い環境での一貫した表示を実現しています。
