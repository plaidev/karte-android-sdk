#!/bin/bash

##################################################
# KARTE Android SDK タグ作成スクリプト
# 
# 指定されたモジュールのタグを作成し、リモートリポジトリにプッシュします
##################################################

# 共通ライブラリを読み込み
source "$(dirname $0)/lib/common.sh"

##################################################
# メイン処理
##################################################

# 引数チェック
if [ $# -eq 0 ]; then
  echo "Usage: $0 <module1> [module2] [module3] ..."
  echo "Example: $0 core debugger notifications"
  exit 1
fi

# 初期化
init_github_operations

# 各モジュールのタグを作成
create_modules_tags "$@"

echo "Tag creation completed."