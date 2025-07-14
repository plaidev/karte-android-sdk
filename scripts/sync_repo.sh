#!/bin/bash

##################################################
# KARTE Android SDK リポジトリ同期スクリプト
# 
# masterブランチをsync_repoに強制プッシュします
##################################################

# 共通ライブラリを読み込み
source "$(dirname $0)/lib/common.sh"

##################################################
# メイン処理
##################################################

# 初期化
init_github_operations

# リポジトリ同期実行
echo "Force pushing master to sync_repo..."
git push -f sync_repo master

echo "Repository sync completed."