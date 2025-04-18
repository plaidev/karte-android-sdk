#!/bin/bash -e

##################################################
# Functions (Sub command functions)
##################################################

function set_remote_repository() {
  EXIST_REMOTE_REPO=`git remote | grep sync_repo | echo $?`
  if [[ $EXIST_REMOTE_REPO == 0 ]]; then
    git remote add sync_repo ${GITHUB_REMOTE_ADDRESS}
    git fetch sync_repo
  fi
}

function set_tag() {
  local TAG=$1
  git tag $TAG
  git push origin $TAG
  git push sync_repo $TAG
}

function has_tag() {
  local TAG=$1
  REMOTE_TAGS=(`git tag`)
  for REMOTE_TAG in ${REMOTE_TAGS[@]}; do
    if [[ $REMOTE_TAG == $TAG ]]; then
      return 1
    fi
  done
  return 0
}

function sync_repository() {
  git push -f sync_repo master
}

function publish() {
  local TARGETS_MODULES=($@)
  if [ -z $TARGETS_MODULES ]; then
    echo "Module is not updated"
    exit 1
  fi

  sync_repository

  for MODULE in ${TARGETS_MODULES[@]}; do
    local TARGET=`echo $MODULE | sed -e "s/\/version//"`
    TAG_VERSION=`ruby scripts/bump_version.rb current-tag -t $TARGET`

    has_tag $TAG_VERSION
    if [ $? -eq 1 ]; then
      echo "This tag is already exist: $TAG_VERSION"
    else
      set_tag $TAG_VERSION
    fi
  done

  for MODULE in ${TARGETS_MODULES[@]}; do
    local TARGET=`echo $MODULE | sed -e "s/\/version//"`
    if [ $TARGET = "gradle-plugin" ]; then
      ./gradlew -p gradle-plugin/ publish
    else
      ./gradlew $TARGET:publish
    fi
  done
  ./gradlew closeAndReleaseRepository

  # Publish release node
  ruby scripts/publish_changelog.rb
}

##################################################
# Checkout
##################################################

set -e
cd `dirname $0`
cd ../

##################################################
# Commands
##################################################

git config --global user.name "${GITHUB_USER_NAME}"
git config --global user.email "${GITHUB_USER_EMAIL}"

set_remote_repository

DIFF_TARGETS=($(git diff --name-only sync_repo/master | grep -E '^(version$|.*/version$)'))

publish ${DIFF_TARGETS[@]}
