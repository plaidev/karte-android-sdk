#!/bin/bash

##################################################
# KARTE Android SDK Maven パブリッシュスクリプト
# 
# 指定されたモジュールをMaven Centralにパブリッシュします
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
init_publish_operations

# Maven パブリッシュ実行
publish_modules "$@"

echo "Maven publish completed."