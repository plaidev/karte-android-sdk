#!/bin/bash

##################################################
# KARTE Android SDK 自動パブリッシュスクリプト
#
# このスクリプトは、変更されたモジュールを自動的に検出し、
# Maven Central にパブリッシュするためのスクリプトです。
##################################################

##################################################
# Git関連関数
##################################################

# タグ存在確認関数
function has_tag() {
  git tag -l "$1" | grep -Fxq "$1"
}

# タグ作成関数
function create_tag() {
  local TAG=$1

  git tag $TAG || echo "Tag $TAG already exists locally"
  git push origin $TAG || echo "Failed to push tag $TAG to origin"
  git push sync_repo $TAG || echo "Failed to push tag $TAG to sync_repo"
}

##################################################
# パブリッシュ関連関数
##################################################

# モジュール名からターゲット名取得
function get_target_name() {
  local MODULE=$1
  echo $MODULE | sed -e "s/\/version//"
}

# Maven パブリッシュ実行
function publish_module() {
  local MODULE=$1

  echo "Publishing module: $MODULE"

  # NOTE: Sonatype close and release can take 10-30 minutes
  # See: https://vanniktech.github.io/gradle-maven-publish-plugin/central/#publishing-releases
  # Setting timeout to 40 minutes (2400 seconds) to ensure successful completion
  if [ "$MODULE" = "gradle-plugin" ]; then
    ./gradlew -p gradle-plugin/ publishAndReleaseToMavenCentral --no-configuration-cache -PSONATYPE_CLOSE_TIMEOUT_SECONDS=2400
  else
    ./gradlew $MODULE:publishAndReleaseToMavenCentral --no-configuration-cache -PSONATYPE_CLOSE_TIMEOUT_SECONDS=2400
  fi
}

# モジュールのタグ作成処理
function create_module_tag() {
  local MODULE=$1
  local TARGET=`get_target_name $MODULE`

  echo "Processing module: $TARGET"

  TAG_VERSION=`ruby scripts/bump_version.rb current-tag -t $TARGET`
  echo "Creating tag: $TAG_VERSION for module: $TARGET"

  if has_tag "$TAG_VERSION"; then
    echo "Tag $TAG_VERSION already exists"
  else
    create_tag "$TAG_VERSION"
  fi
}

# 複数モジュールのタグ作成
function create_modules_tags() {
  local MODULES=("$@")

  for MODULE in "${MODULES[@]}"; do
    create_module_tag "$MODULE"
  done
}

# 複数モジュールのMavenパブリッシュ
function publish_modules() {
  local MODULES=("$@")

  for MODULE in "${MODULES[@]}"; do
    local TARGET=`get_target_name $MODULE`
    publish_module "$TARGET"
  done
}

##################################################
# パブリッシュメイン関数
##################################################

function publish() {
  local TARGETS_MODULES=($@)

  # リポジトリ同期
  echo "Pushing master to sync_repo..."
  git push sync_repo master

  # 各モジュールに対してタグ作成処理を実行
  create_modules_tags "${TARGETS_MODULES[@]}"

  # 各モジュールに対してMaven Centralパブリッシュ処理を実行
  publish_modules "${TARGETS_MODULES[@]}"
}

##################################################
# メイン処理
##################################################

# sync_repo/masterとの差分から変更されたバージョンファイルを特定
DIFF_TARGETS=($(git diff --name-only sync_repo/master | grep -E '^(version$|.*/version$)'))

# 特定されたターゲットモジュールをパブリッシュ
publish ${DIFF_TARGETS[@]}
