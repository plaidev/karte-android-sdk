#!/bin/bash

##################################################
# KARTE Android SDK 共通関数ライブラリ
# 
# パブリッシュ関連スクリプトで共通して使用される関数群
##################################################

##################################################
# Git関連関数
##################################################

# Git設定関数
function setup_git() {
  git config --global user.name "${GITHUB_USER_NAME}"
  git config --global user.email "${GITHUB_USER_EMAIL}"
}

# リモートリポジトリ設定関数
function setup_remote_repository() {
  local EXIST_REMOTE_REPO=`git remote | grep sync_repo | wc -l`
  
  if [[ $EXIST_REMOTE_REPO == 0 ]]; then
    git remote add sync_repo ${GITHUB_REMOTE_ADDRESS}
    git fetch sync_repo
  fi
}

# タグ存在確認関数
function has_tag() {
  local TAG=$1
  
  local REMOTE_TAGS=(`git tag`)
  
  for REMOTE_TAG in ${REMOTE_TAGS[@]}; do
    if [[ $REMOTE_TAG == $TAG ]]; then
      return 1  # タグが存在する場合
    fi
  done
  
  return 0  # タグが存在しない場合
}

# タグ作成関数
function create_tag() {
  local TAG=$1
  
  git tag $TAG || echo "Tag $TAG already exists locally"
  git push origin $TAG || echo "Failed to push tag $TAG to origin"
  git push sync_repo $TAG || echo "Failed to push tag $TAG to sync_repo"
}

##################################################
# 環境変数チェック関数
##################################################

# GitHub関連環境変数チェック
function check_github_env() {
  local required_vars=("GITHUB_REMOTE_ADDRESS" "GITHUB_USER_NAME" "GITHUB_USER_EMAIL")
  
  for var in "${required_vars[@]}"; do
    if [ -z "${!var}" ]; then
      echo "Error: $var environment variable is required"
      exit 1
    fi
  done
}

# 暗号化関連環境変数チェック
function check_encryption_env() {
  local required_vars=("ENCRYPT_KEY" "GPG_KEY")
  
  for var in "${required_vars[@]}"; do
    if [ -z "${!var}" ]; then
      echo "Error: $var environment variable is required"
      exit 1
    fi
  done
}

##################################################
# 暗号化関連関数
##################################################

# プロパティファイル復号化
function decrypt_properties() {
  echo "Decrypting properties..."
  mkdir -p ~/.gradle
  openssl aes-256-cbc -d -md sha512 -pbkdf2 -iter 100000 -salt \
    -in buildscripts/encrypted.properties -k $ENCRYPT_KEY >> ~/.gradle/gradle.properties
}

# GPGキー復号化
function decrypt_gpg_key() {
  echo "Decrypting GPG key..."
  echo "$GPG_KEY" | base64 -d > secret-keys.gpg
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
  
  if [ "$MODULE" = "gradle-plugin" ]; then
    ./gradlew -p gradle-plugin/ publishAndReleaseToMavenCentral --no-configuration-cache
  else
    ./gradlew $MODULE:publishAndReleaseToMavenCentral --no-configuration-cache
  fi
}

# モジュールのタグ作成処理
function create_module_tag() {
  local MODULE=$1
  local TARGET=`get_target_name $MODULE`
  
  echo "Processing module: $TARGET"
  
  TAG_VERSION=`ruby scripts/bump_version.rb current-tag -t $TARGET`
  echo "Creating tag: $TAG_VERSION for module: $TARGET"
  
  has_tag $TAG_VERSION
  if [ $? -eq 1 ]; then
    echo "Tag $TAG_VERSION already exists"
  else
    create_tag $TAG_VERSION
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
# 初期化関数
##################################################

# スクリプト共通初期化
function init_script() {
  # エラー発生時は処理を停止
  set -e
  
  # スクリプトのディレクトリに移動
  cd `dirname $0`
  
  # プロジェクトのルートディレクトリに移動
  cd ../
}

# GitHub系操作の標準初期化
function init_github_operations() {
  init_script
  check_github_env
  setup_git
  setup_remote_repository
}

# パブリッシュ系操作の標準初期化  
function init_publish_operations() {
  init_script
  check_encryption_env
  decrypt_properties
  decrypt_gpg_key
}