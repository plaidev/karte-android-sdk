#!/bin/bash

##################################################
# KARTE Android SDK 自動パブリッシュスクリプト
# 
# このスクリプトは、変更されたモジュールを自動的に検出し、
# Maven Central にパブリッシュするためのスクリプトです。
##################################################

# 共通ライブラリを読み込み
source "$(dirname $0)/lib/common.sh"

##################################################
# パブリッシュメイン関数
##################################################

function publish() {
  local TARGETS_MODULES=($@)
  
  # 対象モジュールが存在しない場合は終了
  if [ -z $TARGETS_MODULES ]; then
    echo "Module is not updated"
    exit 1
  fi

  # リポジトリ同期
  echo "Force pushing master to sync_repo..."
  git push -f sync_repo master

  # 各モジュールに対してタグ作成処理を実行
  create_modules_tags "${TARGETS_MODULES[@]}"

  # 各モジュールに対してMaven Centralパブリッシュ処理を実行
  publish_modules "${TARGETS_MODULES[@]}"
}

##################################################
# メイン処理
##################################################

# 初期化
init_github_operations

# sync_repo/masterとの差分から変更されたバージョンファイルを特定
DIFF_TARGETS=($(git diff --name-only sync_repo/master | grep -E '^(version$|.*/version$)'))

# 特定されたターゲットモジュールをパブリッシュ
publish ${DIFF_TARGETS[@]}
