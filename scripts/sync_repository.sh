#!/bin/bash -e

git config --global user.name "${GITHUB_USER_NAME}"
git config --global user.email "${GITHUB_USER_EMAIL}"

# とりあえずプライベートリポジトリからのみsync
if [[ $EXEC_ENV == private ]]; then
  EXIST_REMOTE_REPO=`git remote | grep sync_repo | echo $?`
  if [[ $EXIST_REMOTE_REPO == 0 ]]; then
    git remote add sync_repo ${GITHUB_REMOTE_ADDRESS}
  fi
  git push -f sync_repo master
elif [[ $EXEC_ENV == public ]]; then
  echo "This execution environment is public"
else
  echo "This execution environment is invalid"
  exit 1
fi
