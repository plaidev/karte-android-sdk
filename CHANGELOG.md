# Latest Version

| モジュール/プラグイン名 | Description | 最新のバージョン |
| :-- | :-- | :-- |
| core | イベントトラッキング機能を提供します。 | 2.22.0 |
| inappmessaging | アプリ内メッセージ機能を提供します。 | 2.17.0 |
| notifications | プッシュ通知の受信および効果測定機能を提供します。 | 2.10.0 |
| variables | 設定値配信機能を提供します。 | 2.4.0 |
| visualtracking | ビジュアルトラッキング機能を提供します。| 2.9.0 |
| inbox | Push通知の送信履歴を取得する機能を提供します（β版）。 | 0.1.0 |
| Karte Gradle Plugin | ビジュアルトラッキング機能に必要なプラグインです。| 2.5.0 |


### InAppMessaging 2.17.0
** 💊FIXED**
- タグv2利用時に、エレメントビルダー（β版）で作成した接客アクションで「アプリで確認」機能が動かない問題を修正しました。

# Releases - 2023.08.18

### InAppMessaging 2.16.1
** 💊FIXED **
- Tracker.viewをUIスレッド以外で呼び出すとクラッシュする不具合を修正しました。

# Releases - 2023.08.10

### Core 2.22.0
** 🎉 FEATURE**
- サブモジュールがイベントを編集できる機能を追加しました。
** 🔨CHANGED**
- Native機能呼び出しをサブモジュールから追加可能にしました。

### InAppMessaging 2.16.0
** 🔨CHANGED**
- タグv2利用時に、Viewイベントをアクション側に連携する機能を追加しました。
- Activityが取得できない時に受信したアクションを破棄せず処理するようにしました。
** 💊FIXED **
- タップ時のパフォーマンスを改善する仕組みに不具合があったので修正しました。

### Notifications 2.10.0
** 🎉 FEATURE**
- Android 13以降において、[Native機能呼び出し](https://support.karte.io/post/1F1VeY2yy3mrTIO2U4HhHi)のプッシュ通知の許可を求めるアラートの表示に対応しました。
  - アプリ内メッセージ経由でのみ呼び出し可能です。（プッシュ通知は権限がないと表示できないため）

### VisualTracking 2.9.0
** 🔨CHANGED**
- モジュール間連携用のインターフェース仕様の変更に合わせて軽微な修正を行いました。

# Releases - 2023.04.11

### Core 2.21.2
** 💊FIXED **
- MessageEventクラスのコンストラクタに非互換な変更が含まれていたのを修正しました。

# Releases - 2023.04.06

### Core 2.21.1
** 💊FIXED **
- Eventクラスのコンストラクタに非互換な変更が含まれていたのを修正しました。

# Releases - 2023.04.04

### Core 2.21.0
** 🎉 FEATURE**
- KARTEプロジェクトのAPIキーをSDKに設定できるようになりました。
  - Inboxモジュールを使用する場合のみ設定が必要です。
- サブモジュールと連携してイベントの送信を行う機構を追加しました。
- ログ出力を停止できるようにしました。

** 🔨CHANGED**
- identifyイベントのuser_idに明示的に空文字が指定された場合に警告を出力するように変更しました。
- Trackerのイベント送信のコールバックを、iOS SDKに合わせUIスレッドで呼び出すようにしました。
- message_openイベントの送信仕様をWebの仕様に合わせるようにしました。
- ログの一時保持・収集機能を廃止しました。

### InAppMessaging 2.15.0
** 🔨CHANGED**
- message_openイベントの送信仕様をWebの仕様に合わせるようにしました。

### Variables 2.4.0
** 🔨CHANGED**
- 効果測定用のイベントにフィールドを追加しました。

### VisualTracking 2.8.0
** 🔨CHANGED**
- モジュール間連携用のインターフェース仕様の変更に合わせて軽微な修正を行いました。

### Inbox 0.1.0
** 🎉 FEATURE**
- Push通知の送信履歴が取得できるモジュールをOSSとして公開しました（β版）。
  - ご利用いただくには別途お手続きが必要です。

# Releases - 2022.11.18

### Karte Gradle Plugin 2.5.0
** 💊FIXED **
- Android Gradle Plugin 8.0以降に対応しました。
- `androidx.navigation`ライブラリを参照しているとビルドできない不具合を修正しました。（[issue](https://github.com/plaidev/karte-android-sdk/issues/19))
  - 解消するにはAndroid Gradle Pluginを7.0.0以降にする必要があります。
- Kotlin 1.7.20 で一部の操作ログが送信されない不具合を修正しました。

** 🔨CHANGED**
- Android Gradle Plugin 3.6.0未満は非対応となりました。

# Releases - 2022.09.09

### Notifications 2.9.1
** 💊FIXED**
- 依存関係の更新によりReact Native SDKでビルドができない問題を修正しました。

### Variables 2.3.1
** 💊FIXED**
- 依存関係の更新によりReact Native SDKでビルドができない問題を修正しました。

### VisualTracking 2.7.1
** 💊FIXED**
- 依存関係の更新によりReact Native SDKでビルドができない問題を修正しました。

# Releases - 2022.09.06

### Core 2.20.0
** 🔨CHANGED**
- minSdkVersionを 14 -> 16 に変更しました。

### InAppMessaging 2.14.0
** 💊FIXED**
- AndroidXに依存していないアプリで、画像アップロード時にクラッシュする不具合を修正しました。

** 🔨CHANGED**
- minSdkVersionを 14 -> 16 に変更しました。

### Notifications 2.9.0
** 🔨CHANGED**
- minSdkVersionを 14 -> 16 に変更しました。
- 依存しているfirebase-messagingのバージョンを変更しました。

### Variables 2.3.0
** 🔨CHANGED**
- minSdkVersionを 14 -> 16 に変更しました。

### VisualTracking 2.7.0
** 🔨CHANGED**
- minSdkVersionを 14 -> 16 に変更しました。

# Releases - 2022.07.28

### Core 2.19.0
** 🎉FEATURE**
- WebView連携のための補助APIとして `UserSync.getUserSyncScript` を追加しました。
  - 返されるスクリプトをWebViewで実行することで、`android.webkit.WebView`以外のWebViewに対してもユーザー連携が可能になります。
  - これに伴い、クエリパラメータ連携API `UserSync.appendUserSyncQueryParameter` は非推奨になります。

# Releases - 2022.06.02

### Core 2.18.0
** 💊FIXED **
- Android Gradle Plugin 4.2.0以降に付属するR8でコードの圧縮を行った際に、実行時エラーが起きる不具合を修正しました。（[issue](https://github.com/plaidev/karte-android-sdk/issues/17)）
   - MessageEventTypeクラスのeventNameプロパティは非推奨になり、ダミーの固定値に変更されました。

### InAppMessaging 2.13.0
** 💊FIXED **
- `KarteApp.renewVisitorId`実行後に接客表示用のWebViewがリークする不具合を修正しました。（[issue](https://github.com/plaidev/karte-android-sdk/issues/18)）

# Releases - 2022.04.05

### Core 2.17.0
** 🎉 FEATURE**
- KARTE SDKでURLを開くためのAPIを追加しました。このAPIはSDK内部での利用を想定しており、通常のSDK利用で使用することはありません。

** 🔨 CHANGED**
- identifyイベントのuser_idとviewイベントのview_nameに空文字が指定された場合でも警告の出力のみでイベントが送信されるよう挙動を変更しました。
   - 尚、user_id無しで送信されたidentifyのフィールドがKARTE上で永続化されるかどうかは[ユーザーデータ管理](https://support.karte.io/post/6Uu930PTyQBc6SVAOEOTYp)プラグインの利用状況に依存します。
   - user_id無しでユーザーに紐づく個人情報以外のフィールドをイベントに付与したい場合は[attribute関数](https://plaidev.github.io/karte-sdk-docs/android/core/latest/core/io.karte.android.tracking/-tracker/index.html#%5Bio.karte.android.tracking%2FTracker%2Fattribute%2F%23java.util.Map%3Cjava.lang.String%2C%3F%3E%2FPointingToDeclaration%2F%2C+io.karte.android.tracking%2FTracker%2Fattribute%2F%23org.json.JSONObject%2FPointingToDeclaration%2F%2C+io.karte.android.tracking%2FTracker%2Fattribute%2F%23java.util.Map%3Cjava.lang.String%2C%3F%3E%23io.karte.android.tracking.TrackCompletion%2FPointingToDeclaration%2F%2C+io.karte.android.tracking%2FTracker%2Fattribute%2F%23org.json.JSONObject%23io.karte.android.tracking.TrackCompletion%2FPointingToDeclaration%2F%5D%2FFunctions%2F96193845)を使用してください。

### InAppMessaging 2.12.0
** 🔨 CHANGED**
- Core 2.17.0で追加されたAPIを利用するように内部処理を修正しました。
  - 挙動の変更はありません。

# Releases - 2022.03.31

### Core 2.16.0
** 🎉 FEATURE**
- launchModeがsingleTopなど、再開時にintentが更新されないActivityでもKARTE SDKのDeeplink処理を行うためのAPIを追加しました。
   - 詳細は[こちら](https://developers.karte.io/docs/appendix-relaunch-activity-android-sdk-v2)を確認してください。

# Releases - 2022.02.21

### Core 2.15.0
** 💊FIXED **
- カスタムイベントとしてViewイベントを送信した際に、PvIdが更新されず、接客のリセット等が聞かない不具合を修正しました。

# Releases - 2022.01.12

### VisualTracking 2.6.0
** 🎉 FEATURE**
- 動的なフィールドの付与に対応しました。
  - 動的フィールドについては[こちら](https://support.karte.io/post/7JbUVotDwZMvl6h3HL9Zt7#6-0)を参考ください。

# Releases - 2021.11.26

### Core 2.14.0
** 🎉 FEATURE**
- attributeイベントを送信するためのAPIを追加しました。
  - attributeイベントとidentifyイベントの使い分けについては[こちら](https://support.karte.io/post/1X39NRwR0HXzCtigtRrbLJ#2-0)を参考ください。

** 🔨 CHANGED**
- identifyイベントにuser_idパラメータの付与を必須にしました。
- identifyイベントのuser_idとviewイベントのview_nameに空文字が指定された場合に、イベントが送信されないようにしました。
- イベント名とフィールド名に非推奨な名前が使われていた場合に、warningログを出力するようにしました。
  - イベント名とフィールド名に関する制限については[こちら](https://developers.karte.io/docs/guide-event#%E3%82%A4%E3%83%99%E3%83%B3%E3%83%88%E3%81%AE%E5%88%B6%E9%99%90)を参考ください。

# Releases - 2021.10.15

### Core 2.13.0
** 🔨CHANGED**
- 解析サーバの負荷低減のために、再送が連続して失敗した場合に一時的に再送しないようにしました。
- 再送の回数を調整しました。

### InAppMessaging 2.11.0
** 💊FIXED **
- 全画面モードとカットアウトモードの特定の組み合わせで接客のレイアウトが崩れる不具合を修正しました。

# Releases - 2021.10.01

### Notifications 2.8.0
** 💊FIXED **
- `MessageHandler.handleMessage()`にdefaultIntentを指定している時、無効なdeeplinkを含む通知を削除した際にもdefaultIntentが発火してしまう不具合を修正しました。
- `targetSdkVersion 30`以降のアプリにおいて、Android 11端末で通知タップ時に外部ブラウザを直接起動できないケースがある不具合を修正しました。
- `targetSdkVersion 31`のアプリにおいて、Android 12端末で通知が表示されない不具合を修正しました。

# Releases - 2021.09.17

### VisualTracking 2.5.0
** 💊FIXED **
- Kotlin 1.5 でコンパイルした際に一部の操作ログが送信されない不具合を修正しました。

### Karte Gradle Plugin 2.4.0
** 💊FIXED **
- Kotlin 1.5 でコンパイルした際に一部の操作ログが送信されない不具合を修正しました。
- `targetSdkVersion 31`のアプリにおいて、Android 12端末にインストールできない不具合を修正しました。（[issue](https://github.com/plaidev/karte-android-sdk/issues/16)）

# Releases - 2021.09.10

### InAppMessaging 2.10.0
** 💊FIXED **
- 全画面モードとカットアウトモードの特定の組み合わせで接客を表示中に画面をタップすると、ずれた位置をタップしたと判定されてしまう不具合を修正しました。

### Notifications 2.7.2
** 💊FIXED **
- Android 7以下で通知タップ時にまれにクラッシュする不具合を修正しました。（[issue](https://github.com/plaidev/karte-android-sdk/issues/15)）

# Releases - 2021.08.20

### Core 2.12.1
** 💊FIXED **
- SDK初期化時のネットワーク疎通確認時に、Android 11を中心とした一部の端末でまれにクラッシュする不具合を修正しました。（[issue](https://github.com/plaidev/karte-android-sdk/issues/14)）

# Releases - 2021.08.05

### InAppMessaging 2.9.1
** 💊FIXED **
- 常駐接客が画面遷移時に表示され続けないケースがある不具合を修正しました。
- Unityにおいて、特定端末での画面回転時に接客のレイアウトが崩れる不具合を修正しました。

# Releases - 2021.07.07

### Notifications 2.7.1
** 💊FIXED **
- Firebase Messaging 22.0.0 を使用していると、クラッシュする不具合を修正しました。（[issue](https://github.com/plaidev/karte-android-sdk/issues/13)）

# Releases - 2021.06.17

### InAppMessaging 2.9.0
** 🔨CHANGED**
- 常駐接客を表示中に画面遷移をすると接客表示イベント(message_open)が発生するように修正しました。

### Karte Gradle Plugin 2.3.0
** 💊FIXED **
- Android Gradle Plugin 4.1.0~4.2.1 を使用していると、ビルドができない不具合を修正しました。

# Releases - 2021.05.20

### Core 2.12.0
** 🎉FEATURE**
- サブモジュールの設定をConfigクラス経由で設定・取得するAPIを追加しました。
- アプリのクラッシュイベントの自動送信をオフにする設定を追加しました。

### Notifications 2.7.0
** 🔨CHANGED**
- モジュール設定の方法をCoreモジュールのConfigクラス経由のものに変更しました。
   以前の方法は非推奨になりました。

# Releases - 2021.05.11

### Core 2.11.1
** 💊FIXED **
- 端末のストレージ不足時にクラッシュしていた問題を修正しました。
  尚、この修正は Core 2.6.1 での修正漏れの対応となります。（[issue](https://github.com/plaidev/karte-android-sdk/issues/5)）（[issue](https://github.com/plaidev/karte-android-sdk/issues/8)）

# Releases - 2021.03.15

### Core 2.11.0
** 💊FIXED **
- SDK初期化時のネットワーク疎通確認時に、まれにクラッシュする不具合を修正しました。（[issue](https://github.com/plaidev/karte-android-sdk/issues/10)）
- Trackリクエスト送信時に、まれにクラッシュする不具合を修正しました。（[issue](https://github.com/plaidev/karte-android-sdk/issues/11)）

### InAppMessaging 2.8.2
** 💊FIXED **
- アプリ内メッセージをボタンで閉じた後、同一画面上でviewイベントをトリガーとしたアプリ内メッセージの再表示ができない問題を修正しました。

# Releases - 2021.03.04

### VisualTracking 2.4.0
** 🎉FEATURE**
- ビジュアルトラッキングのペアリング状態を取得できるインターフェースを公開しました。

# Releases - 2021.02.18

### Core 2.10.0
** 💊FIXED **
- オフラインで設定値取得を呼び出した時に完了処理が呼ばれない問題を修正しました。

### VisualTracking 2.3.0
** 🎉FEATURE**
- ビジュアルトラッキングの操作ログを送信するインターフェースを公開しました。

# Releases - 2021.02.08

### Core 2.9.1
** 💊FIXED **
- 2.9.0でのカスタムオブジェクトが大きすぎる場合のクラッシュ修正において一部修正漏れが存在したため、追加で修正しました。（[issue](https://github.com/plaidev/karte-android-sdk/issues/6)）
- アプリのメモリが少ない際にクラッシュする可能性があったため、修正しました。（[issue](https://github.com/plaidev/karte-android-sdk/issues/12)）
   - ログ収集機能
   - イベントキュー読み込み

### InAppMessaging 2.8.1
** 💊FIXED **
- WebViewアプリがアップデート中にアプリを起動するとクラッシュする可能性がある不具合を修正しました。（[issue](https://github.com/plaidev/karte-android-sdk/issues/7)）

### Notifications 2.6.0
** 🎉FEATURE**
- トリガー配信・ターゲット配信において通知の到達やキャンセルに関する補助イベントを追加しました。

# Releases - 2021.01.06

### Core 2.9.0
** 🔨CHANGED**
- イベントに紐付けるカスタムオブジェクトに内部的な上限を設定しました。

** 💊FIXED **
- 難読化時に内部に存在するサブモジュールがロードされない可能性がある不具合を修正しました。
- イベントに紐付けるカスタムオブジェクトが大きすぎるとクラッシュする可能性がある不具合を修正しました。（[issue](https://github.com/plaidev/karte-android-sdk/issues/6)）
    - こちらの修正は漏れがあるため、2.9.1以降のバージョンをご利用ください。

### InAppMessaging 2.8.0
** 🔨CHANGED**
- 難読化設定ファイルをaar内部に含むようにしました。

# Releases - 2020.12.14

### Core 2.8.0
** 🎉FEATURE**
- SDKの初期化時にapp_keyをリソースファイルから自動で読みこむAPIを追加しました。

### VisualTracking 2.2.0
** 🔨CHANGED**
- 試験的なトラッキングモード時にもビジュアルトラッキングを利用可能にしました。

# Releases - 2020.12.08

### Core 2.7.0
** 🔨CHANGED**
- 過剰なリクエストやリトライを防止するための調整を行いました。

### Notifications 2.5.0
** 💊FIXED **
- 同一接客による通知が複数表示された時、2つ目以降の通知がタップしても反応しない不具合を修正しました。

# Releases - 2020.11.17

### Core 2.6.1
** 💊FIXED **
- 端末のストレージ不足時にクラッシュしていた問題を修正しました。（[issue](https://github.com/plaidev/karte-android-sdk/issues/5))
    - こちらの修正は漏れがあるため、2.11.1以降のバージョンをご利用ください。

# Releases - 2020.09.29

### Core 2.6.0
** 🔨CHANGED**
- 試験的な設定を追加しました。通常のSDK利用において当設定を有効化する必要はありません。

### InAppMessaging 2.7.0
** 💊FIXED **
- SDK内部で参照しているライブラリ名が難読化時に意図しない形式になってしまう不具合を修正しました。

### Notifications 2.4.0
** 💊FIXED **
- SDK内部で参照しているライブラリ名が難読化時に意図しない形式になってしまう不具合を修正しました。

### VisualTracking 2.1.0
** 💊FIXED **
- SDK内部で参照しているライブラリ名が難読化時に意図しない形式になってしまう不具合を修正しました。

# Releases - 2020.09.16
### Core 2.5.0
** 🔨CHANGED**
- サポート改善のため、SDKのログを一時保持・収集するようにしました。
- 一部APIのGenerics指定を緩和しました。

### InAppMessaging 2.6.0
** 🔨CHANGED**
- 接客表示時のVERBOSEなログの表示を調整しました。

### Variables 2.2.0
** 🔨CHANGED**
- SDK内部で参照しているライブラリ名を変更しました。

# Releases - 2020.08.21
### InAppMessaging 2.5.1
** 💊FIXED **
- 接客表示中の画面遷移時やviewイベント発火時に、クラッシュする可能性がある不具合を修正しました。([issue](https://github.com/plaidev/karte-android-sdk/issues/1))

# Releases - 2020.07.22
### InAppMessaging 2.5.0
** 🎉FEATURE**
- アクションを常駐させるオプションに対応しました。
  詳細は[こちら](https://developers.karte.io/docs/appendix-iam-control-android-sdk-v2#%E3%82%A2%E3%83%97%E3%83%AA%E5%86%85%E3%83%A1%E3%83%83%E3%82%BB%E3%83%BC%E3%82%B8%E3%82%92%E5%B8%B8%E9%A7%90%E3%81%95%E3%81%9B%E3%82%8B)をご覧ください

### Notifications 2.3.0
** 🎉FEATURE**
- Map型（通知データ）を引数に取るメソッドを追加しました。

### Karte Gradle Plugin 2.2.0
** 💊FIXED **
- Java9以降のモジュール機能に対応したライブラリを参照するとビルドができない不具合を修正しました。

# Releases - 2020.07.08
### Core 2.4.0
** 🎉FEATURE**
- KARTE固有のURLスキームからNative機能の呼び出しが出来るようになりました。
詳細は[Native機能呼び出し](https://support.karte.io/post/1F1VeY2yy3mrTIO2U4HhHi)をご覧ください。

### InAppMessaging 2.4.0
** 🔨CHANGED**
- Coreの機能追加に伴う内部処理の変更を行いました。

### Notifications 2.2.0
** 🔨CHANGED**
- Coreの機能追加に伴う内部処理の変更を行いました。

# Releases - 2020.07.06
### Core 2.3.2
** 💊FIXED**
- SDK初期化時にネットワーク状態の切り替えが起こると、クラッシュする可能性がある不具合を修正しました。([issue](https://github.com/plaidev/karte-android-sdk/issues/2))

### Karte Gradle Plugin 2.1.0
** 💊FIXED **
- Android Gradle Plugin 4.0.0を使用しているとビルドできない問題を修正しました。

# Releases - 2020.06.25
### Core 2.3.1
** 💊FIXED **
- AndroidManifestにandroid:sharedUserIdを指定した際にライブラリがロードできなくなる問題を修正しました。

# Releases - 2020.06.23
### Core 2.3.0
** 🎉FEATURE**
- ディープリンクによるアプリ流入時に自動で送信するイベントを追加しました。

### InAppMessaging 2.3.0
** 🎉FEATURE**
- 接客の表示制限オプションにより表示が抑制された時に表示抑制イベント（_message_suppressed）を飛ばすようにしました。
これにより接客の表示制限オプションにより接客が抑制されたことを検知できるようになります。

** 💊FIXED **
- カットアウトがある端末でチャット画面の上部が見切れる不具合を修正しました。


# Releases - 2020.05.29
### Core 2.2.0
** 💊FIXED**
- SDK初期化時にネットワーク状態の切り替えが起こると、クラッシュする可能性がある不具合を修正しました。
    - こちらの修正は漏れがあるため、2.3.2以降のバージョンをご利用ください。 ([issue](https://github.com/plaidev/karte-android-sdk/issues/2))

### InAppMessaging 2.2.0
** 💊FIXED**
- Android 7以降のSplit screenモードで表示やタップ位置のズレが生じる問題を修正しました。

### Notifications 2.1.0
** 💊FIXED**
- DryRun設定時にAPIを呼び出すとクラッシュする不具合を修正しました。([issue](https://github.com/plaidev/karte-android-sdk/issues/3))

### Variables 2.1.0
** 🔨CHANGED**
- trackOpen / trackClick のAPIにJSONObjectを受け入れるインターフェースを追加しました。

# Releases - 2020.04.24
### Core 2.1.0
** 🔨CHANGED**
- イベント送信時のリクエストボディをgzip圧縮するよう変更しました。

### InAppMessaging 2.1.0
** 🎉FEATURE**
- 接客表示に利用するhtmlの取得エンドポイントを変更（CDN化）しました。
  この変更により、キャッシュにヒットした場合に初回の接客表示時のパフォーマンスが向上します。
- 接客の表示制限オプションにより表示が抑制された時に表示抑制イベント（_message_suppressed）を飛ばすようにしました。
  これにより接客の表示制限オプションにより接客が抑制されたことを検知できるようになります。

** 💊FIXED**
- WebViewキャッシュ有効時に前回の接客が一瞬表示される現象の対策を追加しました。
- ビジターIDをリセットする際のCookie削除処理で `karte.io` ドメインの Cookie に限定して削除するようにしました。

### VisualTracking 2.0.1
** 🔨CHANGED**
- Androidxへの実行時の依存を廃止しました。


# Releases - 2020.04.07
### Core 2.0.0
** 🎉FEATURE**
- イベントの送信失敗時に再送が行われるようになりました。
  詳細は [FAQ](doc:faq-android-sdk-v2#section-%E9%80%81%E4%BF%A1%E3%81%AB%E5%A4%B1%E6%95%97%E3%81%97%E3%81%9F%E3%82%A4%E3%83%99%E3%83%B3%E3%83%88%E3%81%A9%E3%81%86%E3%81%AA%E3%82%8A%E3%81%BE%E3%81%99%E3%81%8B) をご覧ください。
- 画面サイズの情報を送るようになりました。
  詳細は [イベントに自動追加されるフィールド](doc:appendix-fields-android-sdk-v2) をご覧ください。
- `native_app_open` 等のデフォルトイベントに任意のフィールドを付与できるようになりました。

** 🔨CHANGED**
- インターフェースを全面的に見直しました。
  詳細は [SDK v1からv2のアップグレード方法](doc:appendix-upgrade-android-sdk-v2) をご覧ください。
- 複数アプリケーションキーへの対応を廃止しました。

### InAppMessaging 2.0.0
** 🎉FEATURE**
- Window表示時にフォーカスを当てた状態で表示するかどうか設定できるようになりました。
- Windowの表示や接客の表示・非表示を検知できるようになりました。
  詳細は [アプリ内メッセージを表示する](doc:iam-android-sdk-v2#section-%E3%82%A2%E3%82%AF%E3%82%B7%E3%83%A7%E3%83%B3%E3%81%AE%E7%8A%B6%E6%85%8B%E5%A4%89%E5%8C%96%E3%82%92%E6%A4%9C%E7%9F%A5%E3%81%99%E3%82%8B) をご覧ください。
- アクションのリンクをクリックした時に、アクションを閉じないように設定することができるようになりました。
  詳細は [アクションが非表示となる条件](doc:appendix-action-hidden-condition-android-sdk-v2#section-%E3%82%A2%E3%82%AF%E3%82%B7%E3%83%A7%E3%83%B3%E5%86%85%E3%81%AE%E3%83%AA%E3%83%B3%E3%82%AF%E3%82%AF%E3%83%AA%E3%83%83%E3%82%AF%E3%81%AB%E3%82%88%E3%82%8B%E3%82%A2%E3%82%AF%E3%82%B7%E3%83%A7%E3%83%B3%E3%81%AE%E9%9D%9E%E8%A1%A8%E7%A4%BA%E6%9D%A1%E4%BB%B6) をご覧ください。

** 🔨CHANGED**
- インターフェースを全面的に見直しました。
  詳細は [SDK v1からv2のアップグレード方法](doc:appendix-upgrade-android-sdk-v2) をご覧ください。
- 画面境界を自動で認識するようになりました。
  詳細は [アプリ内メッセージを表示する](doc:iam-android-sdk-v2#section-%E3%82%A2%E3%82%AF%E3%82%B7%E3%83%A7%E3%83%B3%E3%82%92%E8%A1%A8%E7%A4%BA%E3%81%99%E3%82%8B%E7%94%BB%E9%9D%A2%E3%82%92%E9%99%90%E5%AE%9A%E3%81%99%E3%82%8B) をご覧ください。
- `location.href` による遷移時に `shouldOpenURL()` を呼び出すように変更しました。
- WebViewのキャッシュ設定 `enabledWebViewCache` をデフォルトで `true` に変更しました。

### Notifications 2.0.0
** 🔨CHANGED**
- インターフェースを全面的に見直しました。
  詳細は [SDK v1からv2のアップグレード方法](doc:appendix-upgrade-android-sdk-v2) をご覧ください。

### Variables 2.0.0
** 🔨CHANGED**
- インターフェースを全面的に見直しました。
  詳細は [SDK v1からv2のアップグレード方法](doc:appendix-upgrade-android-sdk-v2) をご覧ください。

### VisualTracking 2.0.0
** 🎉FEATURE**
- 同一の階層にある同じ種類のコンポーネントを識別できるようになりました。
- ペアリング中は端末がスリープ状態にならないようにしました。

### Karte Gradle Plugin 2.0.0
** 🔨CHANGED**
- namespace等を変更し、VisualTrackingモジュールの2.0.0に対応しました。

<div style="display:none;">
テンプレ

# Releases - 2022.01.12

### Core 2.0.0
** 🎉FEATURE**
- xxx

** 💊FIXED**
- xxx

** 🔨CHANGED**
- xxx
</div>
