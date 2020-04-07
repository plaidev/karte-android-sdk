#!/bin/bash -e

##################################################
# Functions (Sub command functions)
##################################################

function set_tag() {
  local TAG=$1
  EXIST_REMOTE_REPO=`git remote | grep sync_repo | echo $?`
  if [[ $EXIST_REMOTE_REPO == 0 ]]; then
    git remote add sync_repo ${GITHUB_REMOTE_ADDRESS}
  fi
  git tag $TAG
  git push sync_repo $TAG
}

function publish() {
  local TARGETS_MODULES=$@
  if [ -z $TARGETS_MODULES ]; then
    echo "Module is not updated"
    exit 1
  fi

  for MODULE in $TARGETS_MODULES; do
    local TARGET=`echo $MODULE | sed -e "s/\/version//"`
    TAG_VERSION=`ruby scripts/bump_version.rb current-tag -t $TARGET`

    git tag --contains $TAG_VERSION > /dev/null 2>&1
    if [ $? -eq 0 ]; then
      echo "This tag is already exist: $TAG_VERSION"
      exit 1
    else
      set_tag $TAG_VERSION
      # ./gradlew $TARGET:bintrayUpload
    fi
  done
}

##################################################
# Checkout
##################################################

cd `dirname $0`
cd ../

##################################################
# Commands
##################################################

if [[ $EXEC_ENV == public ]]; then
  echo "This execution environment is public"
  exit 0
fi

git config --global user.name "${GITHUB_USER_NAME}"
git config --global user.email "${GITHUB_USER_EMAIL}"

DIFF_TARGETS=(`git diff --name-only origin/develop | grep version`)

publish $DIFF_TARGETS
